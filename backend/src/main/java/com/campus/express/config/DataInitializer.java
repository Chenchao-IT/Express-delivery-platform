package com.campus.express.config;

import com.campus.express.entity.DeliveryTask;
import com.campus.express.entity.Package;
import com.campus.express.entity.User;
import com.campus.express.entity.VirtualShelf;
import com.campus.express.repository.DeliveryTaskRepository;
import com.campus.express.repository.PackageRepository;
import com.campus.express.repository.UserRepository;
import com.campus.express.repository.VirtualShelfRepository;
import com.campus.express.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VirtualShelfRepository shelfRepository;
    private final PackageRepository packageRepository;
    private final DeliveryTaskRepository deliveryTaskRepository;
    private final PasswordEncoder passwordEncoder;
    private final WalletService walletService;
    private final DemoDataProperties demoDataProperties;

    @Override
    public void run(String... args) {
        initAdmin();
        initShelves();
        initUsers();
        initPackages();
        initTasks();
    }

    private void initAdmin() {
        DemoDataProperties.Admin adminConfig = demoDataProperties.getAdmin();
        User admin = userRepository.findByUsername(adminConfig.getUsername()).orElseGet(User::new);
        admin.setUsername(adminConfig.getUsername());
        admin.setPassword(passwordEncoder.encode(adminConfig.getPassword()));
        admin.setRealName(adminConfig.getRealName());
        admin.setRole(User.UserRole.ADMIN);
        userRepository.save(admin);
    }

    private void initShelves() {
        if (shelfRepository.count() > 0) {
            return;
        }
        for (char zone = 'A'; zone <= 'C'; zone++) {
            for (int row = 1; row <= 3; row++) {
                for (int level = 1; level <= 2; level++) {
                    for (int pos = 1; pos <= 5; pos++) {
                        VirtualShelf shelf = new VirtualShelf();
                        shelf.setShelfCode(String.format("%c-%02d-%d-%03d", zone, row, level, pos));
                        shelf.setZone(String.valueOf(zone));
                        shelf.setRowNum(row);
                        shelf.setLevelNum(level);
                        shelf.setPositionNum(pos);
                        shelf.setGridX((zone - 'A') * 2);
                        shelf.setGridY((row - 1) * 3 + level - 1);
                        shelf.setStatus(VirtualShelf.ShelfStatus.AVAILABLE);
                        shelfRepository.save(shelf);
                    }
                }
            }
        }
    }

    private void initUsers() {
        initCouriers();
        initStudents();
    }

    private void initCouriers() {
        List<DemoDataProperties.Courier> configured = demoDataProperties.getCouriers();
        if (configured == null || configured.isEmpty()) {
            configured = List.of(
                courier("courier1", "张师傅", "13800001101"),
                courier("courier2", "李师傅", "13800001102"),
                courier("courier3", "王师傅", "13800001103")
            );
        }

        for (DemoDataProperties.Courier item : configured) {
            User user = userRepository.findByUsername(item.getUsername()).orElseGet(User::new);
            user.setUsername(item.getUsername());
            user.setPassword(passwordEncoder.encode(passwordOrDefault(item.getPassword())));
            user.setRealName(item.getRealName());
            user.setPhone(item.getPhone());
            user.setRole(User.UserRole.COURIER);
            userRepository.save(user);
            walletService.ensureMinimumBalance(user.getId(), demoDataProperties.getBalance().getCourierMinimum());
        }
    }

    private void initStudents() {
        List<DemoDataProperties.Student> configured = demoDataProperties.getStudents();
        if (configured == null || configured.isEmpty()) {
            configured = List.of(
                student("student1", "张三", "13900001201", "计算机学院", "宿舍1号楼201"),
                student("student2", "李四", "13900001202", "信息学院", "宿舍2号楼202"),
                student("student3", "王五", "13900001203", "电子学院", "宿舍3号楼203"),
                student("student4", "赵六", "13900001204", "经管学院", "宿舍1号楼204"),
                student("student5", "钱七", "13900001205", "外国语学院", "宿舍2号楼205"),
                student("student6", "孙八", "13900001206", "机械学院", "宿舍3号楼206")
            );
        }

        for (DemoDataProperties.Student item : configured) {
            User user = userRepository.findByUsername(item.getUsername()).orElseGet(User::new);
            user.setUsername(item.getUsername());
            user.setPassword(passwordEncoder.encode(passwordOrDefault(item.getPassword())));
            user.setRealName(item.getRealName());
            user.setPhone(item.getPhone());
            user.setCollege(item.getCollege());
            user.setAddress(item.getAddress());
            user.setRole(User.UserRole.STUDENT);
            userRepository.save(user);
            walletService.ensureMinimumBalance(user.getId(), demoDataProperties.getBalance().getStudentMinimum());
        }
    }

    private void initPackages() {
        if (packageRepository.count() > 0) {
            return;
        }

        List<User> students = userRepository.findByRole(User.UserRole.STUDENT);
        List<User> couriers = userRepository.findByRole(User.UserRole.COURIER);
        List<String> shelves = shelfRepository.findAll().stream().map(VirtualShelf::getShelfCode).toList();
        if (students.isEmpty() || shelves.isEmpty()) {
            return;
        }

        int total = Math.max(12, demoDataProperties.getTargets().getPackages());
        Package.PackageSize[] sizes = Package.PackageSize.values();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < total; i++) {
            User student = students.get(i % students.size());
            Package pkg = new Package();
            pkg.setTrackingNumber("SF" + String.format("%012d", 100000000001L + i));
            pkg.setStudentId(student.getId());
            pkg.setCourierId(couriers.isEmpty() ? null : couriers.get(i % couriers.size()).getId());
            pkg.setSize(sizes[i % sizes.length]);
            pkg.setShelfCode(shelves.get(i % shelves.size()));
            pkg.setPickupCode(String.format("%06d", 100000 + i));
            pkg.setStorageTime(now.minusDays(i % 7).minusHours(i));

            int statusIdx = i % 4;
            if (statusIdx == 0) {
                pkg.setStatus(Package.PackageStatus.IN_STORAGE);
            } else if (statusIdx == 1) {
                pkg.setStatus(Package.PackageStatus.OUT_FOR_DELIVERY);
                pkg.setEstimatedDeliveryTime(pkg.getStorageTime().plusHours(6));
            } else if (statusIdx == 2) {
                pkg.setStatus(Package.PackageStatus.DELIVERED);
                pkg.setEstimatedDeliveryTime(pkg.getStorageTime().plusHours(4));
                pkg.setActualDeliveryTime(pkg.getStorageTime().plusHours(8));
            } else {
                pkg.setStatus(Package.PackageStatus.PICKED_UP);
                pkg.setEstimatedDeliveryTime(pkg.getStorageTime().plusHours(4));
                pkg.setActualDeliveryTime(pkg.getStorageTime().plusHours(9));
            }

            packageRepository.save(pkg);
        }
    }

    private void initTasks() {
        if (deliveryTaskRepository.count() > 0) {
            return;
        }

        List<User> students = userRepository.findByRole(User.UserRole.STUDENT);
        List<User> couriers = userRepository.findByRole(User.UserRole.COURIER);
        List<Package> packages = packageRepository.findAll();
        if (students.isEmpty() || couriers.isEmpty() || packages.isEmpty()) {
            return;
        }

        List<String> destinations = demoDataProperties.getDestinations() == null || demoDataProperties.getDestinations().isEmpty()
            ? List.of("DORM_1", "DORM_2", "DORM_3", "CAFETERIA")
            : demoDataProperties.getDestinations();

        int scheduledTarget = Math.max(4, demoDataProperties.getTargets().getScheduledTasks());
        int rewardTarget = Math.max(3, demoDataProperties.getTargets().getRewardTasks());
        int scheduledCount = 0;
        int rewardCount = 0;
        LocalDateTime baseTime = LocalDateTime.now().minusDays(3);

        for (int i = 0; i < packages.size() && (scheduledCount < scheduledTarget || rewardCount < rewardTarget); i++) {
            Package pkg = packages.get(i);
            if (scheduledCount < scheduledTarget
                && (pkg.getStatus() == Package.PackageStatus.OUT_FOR_DELIVERY || pkg.getStatus() == Package.PackageStatus.DELIVERED)) {
                DeliveryTask task = new DeliveryTask();
                task.setType(DeliveryTask.TaskType.SCHEDULED);
                task.setPackageId(pkg.getId());
                task.setPublisherId(pkg.getStudentId());
                task.setCourierId(couriers.get(i % couriers.size()).getId());
                task.setDestination(destinations.get(i % destinations.size()));
                task.setEstimatedDistance(BigDecimal.valueOf(180 + i * 10L));
                task.setEstimatedTime(BigDecimal.valueOf(8 + (i % 10)));
                task.setPathJson("[[0,0],[2,2],[4,4]]");
                task.setPriority(1);
                task.setStatus(pkg.getStatus() == Package.PackageStatus.DELIVERED
                    ? DeliveryTask.TaskStatus.COMPLETED
                    : DeliveryTask.TaskStatus.IN_PROGRESS);
                task.setStartedAt(baseTime.plusHours(i));
                if (task.getStatus() == DeliveryTask.TaskStatus.COMPLETED) {
                    task.setCompletedAt(baseTime.plusHours(i + 2));
                    task.setActualDistance(task.getEstimatedDistance());
                    task.setActualTime(task.getEstimatedTime());
                }
                deliveryTaskRepository.save(task);
                scheduledCount++;
            }

            if (rewardCount < rewardTarget && pkg.getStatus() == Package.PackageStatus.IN_STORAGE) {
                User publisher = students.stream()
                    .filter(student -> student.getId().equals(pkg.getStudentId()))
                    .findFirst()
                    .orElse(students.get(0));

                BigDecimal reward = BigDecimal.valueOf(2 + (i % 5)).setScale(2);
                walletService.ensureMinimumBalance(publisher.getId(), reward);
                walletService.freeze(publisher.getId(), reward);

                DeliveryTask task = new DeliveryTask();
                task.setType(DeliveryTask.TaskType.REWARD);
                task.setPackageId(pkg.getId());
                task.setPublisherId(publisher.getId());
                task.setDestination(destinations.get((i + 1) % destinations.size()));
                task.setRewardAmount(reward);
                task.setPriority(2);
                task.setStatus(i % 2 == 0 ? DeliveryTask.TaskStatus.PENDING : DeliveryTask.TaskStatus.ASSIGNED);
                if (task.getStatus() == DeliveryTask.TaskStatus.ASSIGNED) {
                    task.setCourierId(couriers.get(i % couriers.size()).getId());
                    task.setStartedAt(baseTime.plusHours(i));
                }
                deliveryTaskRepository.save(task);
                rewardCount++;
            }
        }
    }

    private String passwordOrDefault(String value) {
        if (value != null && !value.isBlank()) {
            return value;
        }
        return demoDataProperties.getDefaultPassword();
    }

    private DemoDataProperties.Courier courier(String username, String realName, String phone) {
        DemoDataProperties.Courier courier = new DemoDataProperties.Courier();
        courier.setUsername(username);
        courier.setRealName(realName);
        courier.setPhone(phone);
        return courier;
    }

    private DemoDataProperties.Student student(String username, String realName, String phone, String college, String address) {
        DemoDataProperties.Student student = new DemoDataProperties.Student();
        student.setUsername(username);
        student.setRealName(realName);
        student.setPhone(phone);
        student.setCollege(college);
        student.setAddress(address);
        return student;
    }
}
