package com.babpul.storeservice.store;

import com.babpul.storeservice.store.notice.StoreNotice;
import com.babpul.storeservice.store.schedule.StoreSchedule;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "store")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String address;

    @Column(name = "is_open")
    private boolean isOpen;

    @Column(name = "is_available")
    private boolean isAvailable = true;

    @Column(name = "manual_closed", nullable = false)
    private boolean manualClosed = false;

    @Column(name = "manual_closed_date")
    private LocalDate manualClosedDate;

    @Column(name = "manual_open", nullable = false)
    private boolean manualOpen = false;

    @Column(name = "manual_open_date")
    private LocalDate manualOpenDate;

    private String phone;

    // MSA: User 엔티티 직접 참조 대신 ID와 이름만 저장
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "owner_name")
    private String ownerName;

    @Column(name = "random_price")
    private Integer randomPrice;

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StoreSchedule> schedules;

    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StoreNotice> notices;
}
