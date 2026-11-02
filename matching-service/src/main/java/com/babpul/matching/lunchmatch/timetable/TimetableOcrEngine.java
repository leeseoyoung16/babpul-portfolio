package com.babpul.matching.lunchmatch.timetable;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import org.springframework.stereotype.Component;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TimetableOcrEngine {

    private static final Map<String, String> DAY_MAP = new LinkedHashMap<>();
    private static final Set<String> TIME_LABELS =
            new HashSet<>(Arrays.asList("9","10","11","12","1","2","3","4","5"));

    private static final Pattern ROOM_CODE = Pattern.compile(
        "^[가-힣]{1,4}\\d{2,5}$" +
        "|^[가-힣]{1,4}\\d{2,5}-\\d{1,4}$" +
        "|^[a-zA-Z]{1,3}\\d{1,4}$" +
        "|^[a-zA-Z]{1,2}\\d{1,3}-\\d{1,4}$" +
        "|^\\d{3,5}$" +
        "|^\\d+호$" +
        "|^[가-힣]{2,6}관$" +
        "|^[가-힣]{2,6}호관$" +
        "|^[가-힣]{1,2}\\d+호관$" +
        "|^\\d+/\\d+$"
    );

    private static final java.util.regex.Pattern GPA_PATTERN =
        java.util.regex.Pattern.compile("^\\d+\\.\\d{2,}");

    static {
        DAY_MAP.put("월", "MONDAY");
        DAY_MAP.put("화", "TUESDAY");
        DAY_MAP.put("수", "WEDNESDAY");
        DAY_MAP.put("목", "THURSDAY");
        DAY_MAP.put("금", "FRIDAY");
    }

    public List<TimetableItemDto> parseTimetable(BufferedImage image) throws Exception {
        boolean darkMode = isImageDark(image);
        BufferedImage ocrImage = darkMode ? invertImage(image) : image; // 다크 모드이면 이미지 전처리
        Tesseract tess = buildTesseract();

        Layout layout = analyzeLayout(image, ocrImage, tess, darkMode); // 요일/시간 좌표 인식
        List<Rectangle> blocks = detectSubjectBlocks(image, darkMode, layout); // 색상 블록 검출 및 범위 보정
        List<Map<String, String>> schedules = mapBlocksToSchedules(tess, image, ocrImage, darkMode, layout, blocks); // 과목명 + 요일 + 시간 결정
        postProcessSchedules(schedules); // 중복/노이즈 제거 + 정렬

        log.info("완료: {}개 항목", schedules.size());
        return schedules.stream()
                .map(m -> new TimetableItemDto(
                        m.get("dayOfWeek"),
                        m.get("startTime"),
                        m.get("endTime"),
                        m.get("subjectName")
                ))
                .collect(Collectors.toList());
    }

    /** 요일 헤더 컬럼과 시간 행 좌표 인식. ([1/3]) */
    private static Layout analyzeLayout(BufferedImage image, BufferedImage ocrImage, Tesseract tess, boolean darkMode) throws Exception {
        log.info("[1/3] 레이아웃 분석 중... (darkMode={})", darkMode);
        List<Word> allWords = tess.getWords(ocrImage, ITessAPI.TessPageIteratorLevel.RIL_WORD);

        Map<String, Rectangle> dayHeaderBoxes = new LinkedHashMap<>();
        for (Word w : allWords) {
            String t = w.getText().trim();
            Rectangle b = w.getBoundingBox();
            if (DAY_MAP.containsKey(t)) {
                if (!dayHeaderBoxes.containsKey(t) || b.y < dayHeaderBoxes.get(t).y) {
                    dayHeaderBoxes.put(t, b);
                }
            }
        }
        Map<String, Integer> dayColX = new TreeMap<>();
        int dayHeaderMaxY = 0;
        for (Map.Entry<String, Rectangle> e : dayHeaderBoxes.entrySet()) {
            Rectangle b = e.getValue();
            dayColX.put(e.getKey(), b.x + b.width / 2);
            dayHeaderMaxY = Math.max(dayHeaderMaxY, b.y + b.height);
        }

        if (dayColX.isEmpty()) {
            log.warn("  경고: 요일 헤더를 찾지 못했습니다.");
        }
        log.info("  요일 컬럼: {}", dayColX.keySet());

        int bfsTopBound = dayHeaderMaxY > 0 ? dayHeaderMaxY + 5 : image.getHeight() / 20; // 수업 블록 탐색 위치
        int timeColW = computeTimeColWidth(dayColX, image.getWidth());

        int timeStripW = Math.min(timeColW, image.getWidth() / 12);
        TreeMap<Integer, Integer> timeRowY = new TreeMap<>();
        BufferedImage leftStrip = ocrImage.getSubimage(0, 0, timeStripW, ocrImage.getHeight());
        tess.setLanguage("eng");
        List<Word> leftWords = tess.getWords(leftStrip, ITessAPI.TessPageIteratorLevel.RIL_WORD);
        tess.setLanguage("kor+eng");

        int minTimeY = bfsTopBound;
        for (Word w : leftWords) {
            String t = w.getText().trim();
            Rectangle b = w.getBoundingBox();

            if (b.y > minTimeY && TIME_LABELS.contains(t)) {
                int actual = toActualHour(Integer.parseInt(t));
                timeRowY.putIfAbsent(actual, b.y);
            }
        }
        for (Word w : allWords) { // 빠진 시간 보충
            String t = w.getText().trim();
            Rectangle b = w.getBoundingBox();
            if (b.x + b.width < timeStripW + 10 && b.y > minTimeY && TIME_LABELS.contains(t)) {
                int actual = toActualHour(Integer.parseInt(t));
                timeRowY.putIfAbsent(actual, b.y);
            }
        }

        Layout layout = new Layout();
        layout.allWords = allWords;
        layout.dayColX = dayColX;
        layout.bfsTopBound = bfsTopBound;
        layout.timeColW = timeColW;
        layout.timeRowY = timeRowY;
        return layout;
    }

    /** 색칠된 과목 블록 검출 및 검출 결과에 따른 요일/시간 경계 보정. ([2/3]) */
    private static List<Rectangle> detectSubjectBlocks(BufferedImage image, boolean darkMode, Layout layout) {
        log.info("[2/3] 과목 블록 검출 중...");
        int minBlockPixels = Math.max(200, image.getWidth() * image.getHeight() / 8000); // 최소 200 pixel 이상 블록 후보
        int bfsMinBrightness = darkMode ? 30 : 60; // 밝기
        int bfsMinSat = darkMode ? 8 : 20; // 채도
        List<Rectangle> blocks = detectColoredBlocks(image, 0, layout.bfsTopBound, minBlockPixels, bfsMinBrightness, bfsMinSat);

        if (!blocks.isEmpty()) { // 시간 컬럼 재보정
            int leftmostX = blocks.stream().mapToInt(b -> b.x).min().orElse(layout.timeColW);
            if (leftmostX < layout.timeColW - 10) {
                layout.timeColW = Math.max(20, leftmostX - 5);
            }
        }

        Map<String, Integer> dayColX = layout.dayColX;
        if (dayColX.size() >= 2 && !blocks.isEmpty()) { // 왼쪽 누락 요일 추론
            List<String> DAY_ORDER = Arrays.asList("월", "화", "수", "목", "금");
            int minDayX2 = dayColX.values().stream().mapToInt(Integer::intValue).min().orElse(0);
            int maxDayX2 = dayColX.values().stream().mapToInt(Integer::intValue).max().orElse(image.getWidth());
            int colWidth2 = (maxDayX2 - minDayX2) / (dayColX.size() - 1);
            int leftThreshold = minDayX2 - colWidth2 / 2;
            final int capturedTimeColW = layout.timeColW;
            boolean hasBlocksLeft = blocks.stream()
                    .anyMatch(b -> (b.x + b.width / 2) > capturedTimeColW && (b.x + b.width / 2) < leftThreshold);
            if (hasBlocksLeft) {
                String leftmostDay = dayColX.entrySet().stream()
                        .min(Comparator.comparingInt(Map.Entry::getValue)).get().getKey();
                int leftIdx = DAY_ORDER.indexOf(leftmostDay);
                if (leftIdx > 0) {
                    String missingDay = DAY_ORDER.get(leftIdx - 1);
                    double inferredX = blocks.stream()
                            .filter(b -> (b.x + b.width / 2) > capturedTimeColW && (b.x + b.width / 2) < leftThreshold)
                            .mapToDouble(b -> b.x + b.width / 2.0).average()
                            .orElse(minDayX2 - colWidth2);
                    dayColX.put(missingDay, (int) inferredX);
                    log.info("  미검출 요일 컬럼 추론 (왼쪽): {} @ x={}", missingDay, (int) inferredX);
                }
            }

            int rightThreshold = maxDayX2 + colWidth2 / 2;
            boolean hasBlocksRight = blocks.stream()
                    .anyMatch(b -> (b.x + b.width / 2) > rightThreshold);
            if (hasBlocksRight) { // 오른쪽 누락 요일 추론
                String rightmostDay = dayColX.entrySet().stream()
                        .max(Comparator.comparingInt(Map.Entry::getValue)).get().getKey();
                int rightIdx = DAY_ORDER.indexOf(rightmostDay);
                if (rightIdx >= 0 && rightIdx < DAY_ORDER.size() - 1) {
                    String missingDay = DAY_ORDER.get(rightIdx + 1);
                    double inferredX = blocks.stream()
                            .filter(b -> (b.x + b.width / 2) > rightThreshold)
                            .mapToDouble(b -> b.x + b.width / 2.0).average()
                            .orElse(maxDayX2 + colWidth2);
                    dayColX.put(missingDay, (int) inferredX);
                    log.info("  미검출 요일 컬럼 추론 (오른쪽): {} @ x={}", missingDay, (int) inferredX);
                }
            }
        }

        int timeColW = layout.timeColW;
        int rightBound = image.getWidth();
        if (dayColX.size() >= 2) {
            int minDayX = dayColX.values().stream().mapToInt(Integer::intValue).min().orElse(0);
            int maxDayX = dayColX.values().stream().mapToInt(Integer::intValue).max().orElse(image.getWidth());
            int colWidth = (maxDayX - minDayX) / (dayColX.size() - 1);
            rightBound = maxDayX + colWidth + colWidth / 2;
        }

        int bottomTimeBound = image.getHeight() - image.getHeight() / 25;
        TreeMap<Integer, Integer> timeRowY = layout.timeRowY;
        if (timeRowY.size() >= 2) {
            List<int[]> trows = new ArrayList<>();
            timeRowY.forEach((h, y) -> trows.add(new int[]{h, y}));
            trows.sort(Comparator.comparingInt(a -> a[1]));
            double pph = (double)(trows.get(trows.size()-1)[1] - trows.get(0)[1])
                       / (trows.get(trows.size()-1)[0] - trows.get(0)[0]);
            bottomTimeBound = trows.get(trows.size()-1)[1] + (int)(pph * 1.5);
        }

        blocks.removeIf(b -> b.x + b.width < timeColW + 5);
        final int finalRightBound = rightBound;
        blocks.removeIf(b -> b.x + b.width / 2 > finalRightBound);
        final int finalBottomBound = bottomTimeBound;
        blocks.removeIf(b -> b.y + b.height / 2 > finalBottomBound);
        log.info("  {}개 블록 발견", blocks.size());

        layout.rightBound = rightBound;
        layout.bottomTimeBound = bottomTimeBound;
        return blocks;
    }

    /** 각 블록에 속한 단어를 모아 과목명/요일/시간 확정. ([3/3]) */
    private static List<Map<String, String>> mapBlocksToSchedules(Tesseract tess, BufferedImage image, BufferedImage ocrImage,
                                                                    boolean darkMode, Layout layout, List<Rectangle> blocks) {
        log.info("[3/3] 단어-블록 매핑 중...");
        List<Map<String, String>> schedules = new ArrayList<>();

        for (Rectangle block : blocks) {
            List<Word> inside = new ArrayList<>();
            Rectangle expanded = new Rectangle(block.x - 8, block.y - 8, block.width + 16, block.height + 16);
            for (Word w : layout.allWords) {
                Rectangle wb = w.getBoundingBox();
                int cx = wb.x + wb.width  / 2;
                int cy = wb.y + wb.height / 2;
                if (cx < layout.timeColW || cx > layout.rightBound) continue;
                if (cy < layout.bfsTopBound  || cy > layout.bottomTimeBound) continue;
                if (expanded.contains(cx, cy)) inside.add(w);
            }

            String subject = buildSubjectName(inside); // 노이즈 제거 및 과목명 도출

            int subKor = (int) subject.chars().filter(c -> c >= 0xAC00 && c <= 0xD7A3).count();
            if (subKor == 0) {
                String fallback = fallbackBlockOCR(tess, image, ocrImage, block, darkMode);
                int fbKor = (int) fallback.chars().filter(c -> c >= 0xAC00 && c <= 0xD7A3).count();
                if (fbKor > 0 || (!fallback.isEmpty() && subject.isEmpty())) subject = fallback;
            }
            if (subject.isEmpty()) continue;
            if (subject.length() < 2) continue;
            if (subject.matches("[a-zA-Z]{1,4}")) continue;
            if (GPA_PATTERN.matcher(subject).find()) continue;
            if (subject.matches("[\\d\\s/\\\\.,+\\-=|@#$%^&*()]+")) continue;
            if (subject.matches(".*[\\]\\[|{}].*")) continue;
            if (subject.matches("[a-z]\\s*\\(.*")) continue;

            String day = assignDay(block, layout.dayColX);
            if (day == null) continue;

            String[] times = assignTimes(block, layout.timeRowY);
            if (times == null) continue;
            if (times[0].equals(times[1])) continue;
            if (timeToMinutes(times[1]) - timeToMinutes(times[0]) < 30) continue;

            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("dayOfWeek",   day);
            entry.put("startTime",   times[0]);
            entry.put("endTime",     times[1]);
            entry.put("subjectName", subject.replaceAll("\\s+", ""));
            schedules.add(entry);
        }
        return schedules;
    }

    /** 과목명 정규화, 중복/노이즈 제거, 정렬. */
    private static void postProcessSchedules(List<Map<String, String>> schedules) {
        List<String> distinctSubjects = schedules.stream()
                .map(m -> m.get("subjectName"))
                .distinct()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .collect(Collectors.toList());
        Map<String, String> normalize = new HashMap<>();
        for (int i = 0; i < distinctSubjects.size(); i++) {
            for (int j = i + 1; j < distinctSubjects.size(); j++) {
                String longer  = distinctSubjects.get(i);
                String shorter = distinctSubjects.get(j);
                if (shorter.length() >= 4 && longer.startsWith(shorter)) {
                    normalize.put(shorter, longer);
                }
            }
        }
        if (!normalize.isEmpty()) {
            for (Map<String, String> entry : schedules) {
                String s = entry.get("subjectName");
                if (normalize.containsKey(s)) entry.put("subjectName", normalize.get(s));
            }
        }

        Set<String> seen = new LinkedHashSet<>();
        schedules.removeIf(m -> !seen.add(
                m.get("dayOfWeek") + "|" + m.get("startTime") + "|" + m.get("subjectName")));

        Map<String, Set<String>> subjectTimes = new HashMap<>();
        for (Map<String, String> m : schedules) {
            String subj = m.get("subjectName");
            int korChars = (int) subj.chars().filter(c -> c >= 0xAC00 && c <= 0xD7A3).count();
            if (korChars >= 1 && korChars <= 2) {
                subjectTimes.computeIfAbsent(subj, k -> new HashSet<>())
                            .add(m.get("startTime"));
            }
        }
        Set<String> noiseSubjects = new HashSet<>();
        for (Map.Entry<String, Set<String>> e : subjectTimes.entrySet()) {
            if (e.getValue().size() >= 2) noiseSubjects.add(e.getKey());
        }
        if (!noiseSubjects.isEmpty()) {
            log.info("  OCR 노이즈 과목명 제거: {}", noiseSubjects);
            schedules.removeIf(m -> noiseSubjects.contains(m.get("subjectName")));
        }

        Map<String, Integer> dayOrder = Map.of(
                "MONDAY",0,"TUESDAY",1,"WEDNESDAY",2,"THURSDAY",3,"FRIDAY",4);
        schedules.sort(
                Comparator.comparingInt((Map<String,String> m) ->
                        dayOrder.getOrDefault(m.get("dayOfWeek"), 9))
                .thenComparing(m -> m.get("startTime"))
        );
    }

    /** parseTimetable 단계 간 전달되는 레이아웃 인식 결과. */
    private static class Layout {
        List<Word> allWords;
        Map<String, Integer> dayColX;
        int bfsTopBound;
        int timeColW;
        TreeMap<Integer, Integer> timeRowY;
        int rightBound;
        int bottomTimeBound;
    }

    private static Tesseract buildTesseract() {
        System.setErr(new java.io.PrintStream(java.io.OutputStream.nullOutputStream()));
        Tesseract tess = new Tesseract();
        String tessData = System.getenv("TESSDATA_PREFIX");
        if (tessData == null || tessData.isBlank()) {
            throw new IllegalStateException("TESSDATA_PREFIX 환경변수가 설정되지 않았습니다.");
        }
        tess.setDatapath(tessData);
        tess.setLanguage("kor+eng");
        return tess;
    }

    private static boolean isImageDark(BufferedImage img) {
        int W = img.getWidth(), H = img.getHeight();
        long sum = 0; int count = 0;
        int step = Math.max(1, Math.min(W, H) / 30);
        for (int y = 0; y < H; y += step)
            for (int x = 0; x < W; x += step) {
                int rgb = img.getRGB(x, y);
                sum += ((rgb>>16)&0xFF) + ((rgb>>8)&0xFF) + (rgb&0xFF);
                count += 3;
            }
        return count > 0 && sum / count < 100;
    }

    private static BufferedImage invertImage(BufferedImage src) {
        int W = src.getWidth(), H = src.getHeight();
        int[] pixels = src.getRGB(0, 0, W, H, null, 0, W);
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            pixels[i] = (p & 0xFF000000)
                    | ((255 - ((p>>16)&0xFF)) << 16)
                    | ((255 - ((p>>8)&0xFF)) << 8)
                    | (255 - (p&0xFF));
        }
        BufferedImage result = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        result.setRGB(0, 0, W, H, pixels, 0, W);
        return result;
    }

    private static int computeTimeColWidth(Map<String, Integer> dayColX, int imageWidth) { // 시간 표시 영역 탐색
        if (dayColX.size() >= 2) {
            int minX = dayColX.values().stream().mapToInt(Integer::intValue).min().orElse(imageWidth/5);
            int maxX = dayColX.values().stream().mapToInt(Integer::intValue).max().orElse(imageWidth*4/5);
            int colSpan = maxX - minX;
            int numCols = dayColX.size() - 1;
            int colWidth = numCols > 0 ? colSpan / numCols : colSpan;
            int leftEdgeOfFirstCol = minX - colWidth / 2;
            return Math.max(20, leftEdgeOfFirstCol - 5);
        }
        return imageWidth / 11;
    }

    private static int toActualHour(int grid) { return (grid >= 9) ? grid : grid + 12; }

    private static List<Rectangle> detectColoredBlocks(BufferedImage img,
                                                        int leftBound, int topBound,
                                                        int minPixels, int minBrightness, int minSat) {
        int W = img.getWidth(), H = img.getHeight();
        int[] pixels  = img.getRGB(0, 0, W, H, null, 0, W);
        boolean[] vis = new boolean[W * H];
        List<Rectangle> result = new ArrayList<>();
        int[] dx = {1,-1,0,0}, dy = {0,0,1,-1};

        for (int sy = topBound; sy < H; sy++) {
            for (int sx = leftBound; sx < W; sx++) {
                int idx = sy * W + sx;
                if (vis[idx] || !isColored(pixels[idx], minBrightness, minSat)) { vis[idx] = true; continue; }

                ArrayDeque<Integer> q = new ArrayDeque<>();
                q.add(idx); vis[idx] = true;
                int minX = sx, maxX = sx, minY = sy, maxY = sy, count = 0;

                while (!q.isEmpty()) {
                    int cur = q.poll();
                    int cx = cur % W, cy = cur / W;
                    if (cx < minX) minX = cx; if (cx > maxX) maxX = cx;
                    if (cy < minY) minY = cy; if (cy > maxY) maxY = cy;
                    count++;
                    for (int d = 0; d < 4; d++) {
                        int nx = cx+dx[d], ny = cy+dy[d];
                        if (nx >= 0 && nx < W && ny >= topBound && ny < H) {
                            int ni = ny*W+nx;
                            if (!vis[ni] && isColored(pixels[ni], minBrightness, minSat)) { vis[ni]=true; q.add(ni); }
                        }
                    }
                }
                if (count >= minPixels)
                    result.add(new Rectangle(minX, minY, maxX-minX+1, maxY-minY+1));
            }
        }
        return result;
    }

    private static boolean isColored(int rgb, int minBrightness, int minSat) {
        int r = (rgb>>16)&0xFF, g = (rgb>>8)&0xFF, b = rgb&0xFF;
        int brightness = (r + g + b) / 3;
        int sat = Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b));
        return brightness > minBrightness && brightness < 225 && sat > minSat;
    }

    private static String fallbackBlockOCR(Tesseract tess, BufferedImage original,
                                            BufferedImage ocrImage, Rectangle block, boolean darkMode) {
        String best = "";
        int bestKor = 0;

        try {
            int x = Math.max(0, block.x);
            int y = Math.max(0, block.y);
            int w = Math.min(original.getWidth()  - x, block.width);
            int h = Math.min(original.getHeight() - y, block.height);
            if (w < 5 || h < 5) throw new Exception("block too small");

            BufferedImage gray = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
            java.awt.Graphics2D gg = gray.createGraphics();
            gg.drawImage(original.getSubimage(x, y, w, h), 0, 0, null);
            gg.dispose();
            int[] gp = new int[w * h];
            gray.getRaster().getPixels(0, 0, w, h, gp);
            gp = stretchHistogram(gp);

            int otsu = computeOtsuThreshold(gp);
            long darkCnt = 0, brightCnt = 0;
            for (int p : gp) { if (p <= otsu) darkCnt++; else brightCnt++; }
            boolean textIsBright = darkCnt > brightCnt;
            int[] bin = new int[gp.length];
            for (int i = 0; i < gp.length; i++) {
                boolean isText = textIsBright ? gp[i] > otsu : gp[i] <= otsu;
                bin[i] = isText ? 0x000000 : 0xFFFFFF;
            }
            BufferedImage binary = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            binary.setRGB(0, 0, w, h, bin, 0, w);

            String r = ocrBlock(tess, binary);
            int kor = korCount(r);
            if (kor > bestKor || (kor == bestKor && !r.isEmpty()
                    && (best.isEmpty() || r.length() < best.length()))) {
                best = r; bestKor = kor;
            }
        } catch (Exception ignored) {}

        if (ocrImage != null && bestKor == 0) {
            try {
                int x = Math.max(0, block.x);
                int y = Math.max(0, block.y);
                int w = Math.min(ocrImage.getWidth()  - x, block.width);
                int h = Math.min(ocrImage.getHeight() - y, block.height);
                if (w < 5 || h < 5) throw new Exception("block too small");

                BufferedImage gray2 = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
                java.awt.Graphics2D gg2 = gray2.createGraphics();
                gg2.drawImage(ocrImage.getSubimage(x, y, w, h), 0, 0, null);
                gg2.dispose();
                int[] gp = new int[w * h];
                gray2.getRaster().getPixels(0, 0, w, h, gp);
                gp = stretchHistogram(gp);

                int otsu = computeOtsuThreshold(gp);
                long darkCnt = 0, brightCnt = 0;
                for (int p : gp) { if (p <= otsu) darkCnt++; else brightCnt++; }
                boolean textIsBright = darkCnt > brightCnt;
                int[] bin = new int[gp.length];
                for (int i = 0; i < gp.length; i++) {
                    boolean isText = textIsBright ? gp[i] > otsu : gp[i] <= otsu;
                    bin[i] = isText ? 0x000000 : 0xFFFFFF;
                }
                BufferedImage binary = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                binary.setRGB(0, 0, w, h, bin, 0, w);

                String r = ocrBlock(tess, binary);
                int kor = korCount(r);
                if (kor > bestKor || (kor == bestKor && !r.isEmpty()
                        && (best.isEmpty() || r.length() < best.length()))) {
                    best = r; bestKor = kor;
                }
            } catch (Exception ignored) {}
        }

        return best;
    }

    private static String ocrBlock(Tesseract tess, BufferedImage img) throws Exception {
        int[] psms = { ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK,
                       ITessAPI.TessPageSegMode.PSM_SINGLE_LINE };
        String best = "";
        int bestKor = 0;
        for (int scale : new int[]{3, 4}) {
            if (scale == 4 && bestKor > 0) break;
            BufferedImage scaled = addWhitePadding(scaleUp(img, scale), 8);
            for (int psm : psms) {
                tess.setPageSegMode(psm);
                String raw = tess.doOCR(scaled).trim();
                String cleaned = cleanFallbackText(raw);
                int kor = korCount(cleaned);
                if (kor > bestKor || (kor == bestKor && !cleaned.isEmpty()
                        && (best.isEmpty() || cleaned.length() < best.length()))) {
                    best = cleaned; bestKor = kor;
                }
            }
        }
        tess.setPageSegMode(ITessAPI.TessPageSegMode.PSM_AUTO);
        return best;
    }

    private static BufferedImage addWhitePadding(BufferedImage src, int pad) {
        int nw = src.getWidth() + pad * 2, nh = src.getHeight() + pad * 2;
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = out.createGraphics();
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, nw, nh);
        g.drawImage(src, pad, pad, null);
        g.dispose();
        return out;
    }

    private static int[] stretchHistogram(int[] pixels) {
        int min = 255, max = 0;
        for (int p : pixels) { int v = p & 0xFF; if (v < min) min = v; if (v > max) max = v; }
        if (max - min < 10) return pixels;
        int[] out = new int[pixels.length];
        double scale = 255.0 / (max - min);
        for (int i = 0; i < pixels.length; i++)
            out[i] = (int) Math.min(255, ((pixels[i] & 0xFF) - min) * scale);
        return out;
    }

    private static int computeOtsuThreshold(int[] pixels) {
        int[] hist = new int[256];
        for (int p : pixels) hist[p & 0xFF]++;
        int total = pixels.length;
        double sum = 0;
        for (int i = 0; i < 256; i++) sum += i * hist[i];
        double sumB = 0, wB = 0;
        double maxVar = 0;
        int threshold = 128;
        for (int t = 0; t < 256; t++) {
            wB += hist[t];
            if (wB == 0) continue;
            double wF = total - wB;
            if (wF == 0) break;
            sumB += t * hist[t];
            double mB = sumB / wB;
            double mF = (sum - sumB) / wF;
            double var = wB * wF * (mB - mF) * (mB - mF);
            if (var > maxVar) { maxVar = var; threshold = t; }
        }
        return threshold;
    }

    private static int korCount(String s) {
        return (int) s.chars().filter(c -> c >= 0xAC00 && c <= 0xD7A3).count();
    }

    private static BufferedImage scaleUp(BufferedImage src, int factor) {
        int nw = src.getWidth() * factor, nh = src.getHeight() * factor;
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = out.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }

    private static String cleanFallbackText(String raw) {
        StringBuilder sb = new StringBuilder();
        for (String line : raw.split("[\\n\\r]+")) {
            String t = line.trim();
            if (t.isEmpty()) continue;
            if (GPA_PATTERN.matcher(t).find()) continue;
            if (t.contains("캠퍼스") || t.endsWith("호관")) continue;
            if (ROOM_CODE.matcher(t).matches()) continue;
            String truncated = truncateAtRoomCode(t);
            if (truncated.isEmpty()) continue;
            String cleaned = truncated;
            cleaned = cleaned.replaceAll("^[^가-힣a-zA-Z0-9]+", "").trim();
            cleaned = cleaned.replaceAll("\\(\\d+\\)?$", "").trim();
            cleaned = cleaned.replaceAll("[^가-힣a-zA-Z0-9()\\s]+$", "").trim();
            if (cleaned.length() >= 2) sb.append(cleaned);
        }
        return sb.toString().trim();
    }

    private static String buildSubjectName(List<Word> wordsIn) {
        if (wordsIn.isEmpty()) return "";

        List<Word> words = new ArrayList<>(wordsIn);
        words.sort(Comparator.comparingInt(w -> w.getBoundingBox().y + w.getBoundingBox().height / 2));

        int threshold = 20;
        List<List<Word>> lines = new ArrayList<>();
        List<Word> cur = new ArrayList<>();
        int lastCY = -1000;
        for (Word w : words) {
            int cy = w.getBoundingBox().y + w.getBoundingBox().height / 2;
            if (lastCY >= 0 && Math.abs(cy - lastCY) > threshold) {
                lines.add(new ArrayList<>(cur)); cur.clear();
            }
            cur.add(w); lastCY = cy;
        }
        if (!cur.isEmpty()) lines.add(cur);

        StringBuilder sb = new StringBuilder();
        for (List<Word> line : lines) { // 노이즈 제거
            line.sort(Comparator.comparingInt(w -> w.getBoundingBox().x));
            for (int wi = 0; wi < line.size(); wi++) {
                String t = line.get(wi).getText().trim();
                if (t.isEmpty()) continue;
                if (ROOM_CODE.matcher(t).matches()
                    || t.contains("캠퍼스")
                    || t.matches(".*호관$")
                    || GPA_PATTERN.matcher(t).find()) break;
                if (t.matches("[^가-힣a-zA-Z0-9()]+")) continue;
                if (t.matches("\\d{2,}")) continue;
                if (t.matches("[가-힣]")) {
                    boolean nextIsCode = wi + 1 < line.size() && (
                        ROOM_CODE.matcher(line.get(wi + 1).getText().trim()).matches()
                        || GPA_PATTERN.matcher(line.get(wi + 1).getText().trim()).find());
                    if (nextIsCode) continue;
                }
                if (sb.length() > 0) sb.append(" ");
                sb.append(t);
            }
        }
        String result = sb.toString().trim();
        result = result.replaceAll("(?<=[가-힣])\\s*[a-zA-Z]{2,}\\s*(?=[가-힣])", "").trim();
        result = result.replaceAll("^[^가-힣a-zA-Z0-9]+", "").trim();
        result = result.replaceAll("\\(\\d+\\)?$", "").trim();
        result = deduplicateText(result);
        return result;
    }

    private static String truncateAtRoomCode(String line) {
        StringBuilder sb = new StringBuilder();
        for (String w : line.split("\\s+")) {
            if (w.isEmpty()) continue;
            if (ROOM_CODE.matcher(w).matches()
                || w.contains("캠퍼스")
                || w.matches(".*호관$")
                || GPA_PATTERN.matcher(w).find()) break;
            if (sb.length() > 0) sb.append(" ");
            sb.append(w);
        }
        return sb.toString().trim();
    }

    private static String deduplicateText(String s) {
        if (s.length() < 3) return s;
        int half = s.length() / 2;
        if (s.length() % 2 == 0 && s.substring(0, half).equals(s.substring(half))) {
            return s.substring(0, half);
        }
        return s;
    }

    private static String assignDay(Rectangle block, Map<String, Integer> dayColX) {
        if (dayColX.isEmpty()) return null;
        int cx = block.x + block.width / 2;
        String best = null; int bestDist = Integer.MAX_VALUE;
        for (Map.Entry<String,Integer> e : dayColX.entrySet()) {
            int d = Math.abs(cx - e.getValue());
            if (d < bestDist) { bestDist = d; best = e.getKey(); }
        }
        return best == null ? null : DAY_MAP.get(best);
    }

    private static String[] assignTimes(Rectangle block, TreeMap<Integer, Integer> timeRowY) {
        if (timeRowY.size() < 2) return null;

        List<int[]> rows = new ArrayList<>();
        for (Map.Entry<Integer,Integer> e : timeRowY.entrySet())
            rows.add(new int[]{e.getKey(), e.getValue()});
        rows.sort(Comparator.comparingInt(a -> a[1]));

        double pixPerHour = (double)(rows.get(rows.size()-1)[1] - rows.get(0)[1])
                          / (rows.get(rows.size()-1)[0] - rows.get(0)[0]);
        if (pixPerHour <= 0) return null;
        int baseHour = rows.get(0)[0], baseY = rows.get(0)[1];

        double startFrac = baseHour + (block.y              - baseY) / pixPerHour;
        double endFrac   = baseHour + (block.y + block.height - baseY) / pixPerHour;

        return new String[]{ fracToTime(startFrac), fracToTime(endFrac) };
    }

    private static int timeToMinutes(String hhmm) {
        String[] parts = hhmm.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private static String fracToTime(double frac) {
        int hour = (int) frac;
        double minFrac = (frac - hour) * 60;
        int min = (int) (Math.round(minFrac / 15.0) * 15);
        if (min >= 60) { min = 0; hour++; }
        return String.format("%02d:%02d:00", Math.max(0, Math.min(23, hour)), min);
    }
}