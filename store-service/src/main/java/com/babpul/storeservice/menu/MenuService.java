package com.babpul.storeservice.menu;

import com.babpul.storeservice.store.StoreRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;
    private final StoreRepository storeRepository;

    @Value("${file.upload-dir:./img/uploads/}")
    private String uploadDir;

    @Transactional
    public Menu createMenu(Long storeId, Long ownerId, String name, int price, int prepTime, MultipartFile image) {
        verifyOwner(storeId, ownerId);

        Menu menu = new Menu();
        menu.setStoreId(storeId);
        menu.setName(name);
        menu.setPrice(price);
        menu.setPrepTime(prepTime);
        menu.setAvailable(true);
        Menu saved = menuRepository.save(menu);

        if (image != null && !image.isEmpty()) {
            saved.setImgUrl(saveImage(image, saved.getId()));
            menuRepository.save(saved);
        }
        updateRandomPrice(storeId);
        return saved;
    }

    @Transactional
    public Menu updateMenu(Long menuId, Long ownerId, String name, int price, int prepTime, MultipartFile image) {
        Menu menu = getMenu(menuId);
        verifyOwner(menu.getStoreId(), ownerId);

        menu.setName(name);
        menu.setPrice(price);
        menu.setPrepTime(prepTime);
        if (image != null && !image.isEmpty()) {
            menu.setImgUrl(saveImage(image, menu.getId()));
        }
        Menu saved = menuRepository.save(menu);
        updateRandomPrice(menu.getStoreId());
        return saved;
    }

    @Transactional
    public void disableMenu(Long menuId, Long ownerId) { // 단종 여부
        Menu menu = getMenu(menuId);
        verifyOwner(menu.getStoreId(), ownerId);
        menu.setAvailable(false);
        menuRepository.save(menu);
        updateRandomPrice(menu.getStoreId());
    }

    @Transactional
    public Menu toggleSoldOut(Long menuId, Long ownerId) {
        Menu menu = getMenu(menuId);
        verifyOwner(menu.getStoreId(), ownerId);
        menu.setSoldout(!menu.isSoldout());
        Menu saved = menuRepository.save(menu);
        updateRandomPrice(menu.getStoreId());
        return saved;
    }

    @Transactional
    public Menu updateWeight(Long menuId, int weight, String role) {
        if (!"ADMIN".equals(role)) throw new RuntimeException("관리자만 가중치를 수정할 수 있습니다.");
        if (weight < 1) throw new IllegalArgumentException("가중치는 1 이상이어야 합니다.");
        Menu menu = getMenu(menuId);
        menu.setWeight(weight);
        return menuRepository.save(menu);
    }

    public List<Menu> getMenusByStore(Long storeId) {
        return menuRepository.findByStoreIdAndIsAvailableTrue(storeId);
    }

    private void updateRandomPrice(Long storeId) {
        List<Menu> menus = menuRepository.findByStoreIdAndIsAvailableTrueAndIsSoldoutFalse(storeId);
        int avgPrice = (int) Math.round(
                menus.stream()
                        .mapToInt(Menu::getPrice)
                        .average()
                        .orElse(0)
        );
        storeRepository.findById(storeId).ifPresent(store -> {
            store.setRandomPrice(avgPrice);
            storeRepository.save(store);
        });
    }

    public List<Menu> getOrderableMenus(Long storeId) {
        return menuRepository.findByStoreIdAndIsAvailableTrueAndIsSoldoutFalse(storeId);
    }

    public Menu getMenu(Long menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new RuntimeException("해당 메뉴를 찾을 수 없습니다."));
    }

    private void verifyOwner(Long storeId, Long ownerId) {
        storeRepository.findById(storeId)
                .filter(s -> s.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new RuntimeException("해당 상점의 사장님만 메뉴를 수정할 수 있습니다."));
    }

    private String saveImage(MultipartFile file, Long menuId) {
        try {
            File dir = new File(uploadDir + "/menus/");
            if (!dir.exists()) dir.mkdirs();
            String ext = "";
            String original = file.getOriginalFilename();
            if (original != null && original.contains(".")) ext = original.substring(original.lastIndexOf("."));
            String filename = UUID.randomUUID() + "_" + menuId + ext;
            file.transferTo(new File(dir, filename));
            return "/uploads/menus/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("이미지 업로드 실패: " + e.getMessage());
        }
    }
}