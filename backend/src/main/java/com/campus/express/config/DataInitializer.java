package com.campus.express.config;

import com.campus.express.entity.DeliveryTask;
import com.campus.express.entity.Package;
import com.campus.express.entity.User;
import com.campus.express.entity.UserWallet;
import com.campus.express.entity.VirtualShelf;
import com.campus.express.repository.DeliveryTaskRepository;
import com.campus.express.repository.PackageRepository;
import com.campus.express.repository.UserRepository;
import com.campus.express.repository.UserWalletRepository;
import com.campus.express.repository.VirtualShelfRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VirtualShelfRepository shelfRepository;
    private final PackageRepository packageRepository;
    private final DeliveryTaskRepository deliveryTaskRepository;
    private final UserWalletRepository userWalletRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEMO_PASSWORD = "123456";
    private static final int DEMO_PACKAGE_TARGET = 1200;
    private static final int DEMO_DELIVERY_TASK_TARGET = 900;
    private static final int DEMO_REWARD_TASK_TARGET = 300;

    @Override
    public void run(String... args) {
        initAdmin();
        initShelves();
        initUsers();
        initPackagesToTarget(DEMO_PACKAGE_TARGET);
        initTasksToTarget(DEMO_DELIVERY_TASK_TARGET, DEMO_REWARD_TASK_TARGET);
    }

    private void initAdmin() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRealName("系统管理员");
            admin.setRole(User.UserRole.ADMIN);
            userRepository.save(admin);
        }
    }

    private void initShelves() {
        if (shelfRepository.count() == 0) {
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
    }

    private void initUsers() {
        String encoded = passwordEncoder.encode(DEMO_PASSWORD);

        // 快递员（新建或重置密码，确保 123456 可用）
        for (int i = 1; i <= 3; i++) {
            User courier = userRepository.findByUsername("courier" + i).orElse(new User());
            courier.setUsername("courier" + i);
            courier.setPassword(encoded);
            courier.setRealName(new String[]{"张师傅", "李师傅", "王师傅"}[i - 1]);
            courier.setRole(User.UserRole.COURIER);
            courier.setPhone("1380000110" + i);
            courier = userRepository.save(courier);
            ensureWalletAtLeast(courier.getId(), new BigDecimal("200.00"));
        }

        // 学生（新建或重置密码，确保 123456 可用）
        String[] studentNames = {"张三", "李四", "王五", "赵六", "钱七", "孙八", "周九", "吴十", "郑十一", "王芳", "李磊"};
        String[] colleges = {"计算机学院", "信息学院", "电子学院", "机械学院", "经管学院", "外国语学院"};
        for (int i = 0; i < studentNames.length; i++) {
            User student = userRepository.findByUsername("student" + (i + 1)).orElse(new User());
            student.setUsername("student" + (i + 1));
            student.setPassword(encoded);
            student.setRealName(studentNames[i]);
            student.setRole(User.UserRole.STUDENT);
            student.setPhone("1390000123" + String.format("%02d", i + 1));
            student.setCollege(colleges[i % colleges.length]);
            student.setAddress("宿舍" + ((i % 3) + 1) + "号楼" + (200 + i) + "室");
            student = userRepository.save(student);
            ensureWalletAtLeast(student.getId(), new BigDecimal("500.00"));
        }
    }

    private void ensureWalletAtLeast(Long userId, BigDecimal minBalance) {
        userWalletRepository.findByUserId(userId).map(w -> {
            if (w.getBalance() == null) w.setBalance(BigDecimal.ZERO);
            if (w.getFrozen() == null) w.setFrozen(BigDecimal.ZERO);
            if (w.getBalance().compareTo(minBalance) < 0) {
                w.setBalance(minBalance);
            }
            return userWalletRepository.save(w);
        }).orElseGet(() -> {
            UserWallet w = new UserWallet();
            w.setUserId(userId);
            w.setBalance(minBalance);
            w.setFrozen(BigDecimal.ZERO);
            return userWalletRepository.save(w);
        });
    }

    private void initPackagesToTarget(int target) {
        long existing = packageRepository.count();
        if (existing >= target) return;

        List<User> students = userRepository.findAll().stream()
            .filter(u -> u.getRole() == User.UserRole.STUDENT).toList();
        List<String> shelfCodes = shelfRepository.findAll().stream()
            .map(VirtualShelf::getShelfCode).toList();
        if (students.isEmpty() || shelfCodes.isEmpty()) return;

        Package.PackageSize[] sizes = Package.PackageSize.values();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(30);

        long start = existing + 1;
        long end = target;
        for (long i = start; i <= end; i++) {
            Package pkg = new Package();
            pkg.setTrackingNumber("SF" + String.format("%012d", 100000000000L + i));
            pkg.setStudentId(students.get((int) (i % students.size())).getId());
            pkg.setSize(sizes[(int) (i % sizes.length)]);
            pkg.setShelfCode(shelfCodes.get((int) (i % shelfCodes.size())));
            pkg.setStorageTime(baseTime.plusHours(i % (24 * 25)));

            int statusIdx = (int) (i % 10);
            if (statusIdx < 5) pkg.setStatus(Package.PackageStatus.IN_STORAGE);
            else if (statusIdx < 7) pkg.setStatus(Package.PackageStatus.OUT_FOR_DELIVERY);
            else if (statusIdx < 9) pkg.setStatus(Package.PackageStatus.DELIVERED);
            else pkg.setStatus(Package.PackageStatus.COMPLETED);

            if (pkg.getStatus() != Package.PackageStatus.IN_STORAGE) {
                pkg.setEstimatedDeliveryTime(pkg.getStorageTime().plusHours(6));
            }
            if (pkg.getStatus() == Package.PackageStatus.COMPLETED || pkg.getStatus() == Package.PackageStatus.DELIVERED) {
                pkg.setActualDeliveryTime(pkg.getStorageTime().plusHours(12));
            }
            packageRepository.save(pkg);
        }
    }

    private void initTasksToTarget(int deliveryTaskTarget, int rewardTaskTarget) {
        long existingTasks = deliveryTaskRepository.count();
        List<Package> allPackages = packageRepository.findAll();
        if (allPackages.isEmpty()) return;

        List<User> couriers = userRepository.findAll().stream()
            .filter(u -> u.getRole() == User.UserRole.COURIER).toList();
        List<User> students = userRepository.findAll().stream()
            .filter(u -> u.getRole() == User.UserRole.STUDENT).toList();
        if (couriers.isEmpty() || students.isEmpty()) return;

        String[] destinations = {"DORM_1", "DORM_2", "DORM_3", "CAFETERIA"};
        LocalDateTime baseTime = LocalDateTime.now().minusDays(14);

        // 1) 普通配送任务（SCHEDULED）
        if (existingTasks < deliveryTaskTarget) {
            int need = (int) (deliveryTaskTarget - existingTasks);
            int created = 0;
            for (int i = 0; i < allPackages.size() && created < need; i++) {
                Package pkg = allPackages.get(i);
                // 只对“配送中相关”的包裹生成任务，避免状态太乱
                if (pkg.getStatus() != Package.PackageStatus.OUT_FOR_DELIVERY && pkg.getStatus() != Package.PackageStatus.DELIVERED) {
                    continue;
                }
                DeliveryTask task = new DeliveryTask();
                task.setType(DeliveryTask.TaskType.SCHEDULED);
                task.setPackageId(pkg.getId());
                task.setDestination(destinations[i % destinations.length]);
                task.setPathJson("[{\"x\":0,\"y\":0},{\"x\":3,\"y\":3}]");
                task.setEstimatedDistance(BigDecimal.valueOf(120 + (i % 600)));
                task.setEstimatedTime(BigDecimal.valueOf(5 + (i % 25)));
                task.setPriority(1 + (i % 3));
                task.setCreatedAt(baseTime.plusMinutes(i * 3L));

                DeliveryTask.TaskStatus status = switch (i % 4) {
                    case 0 -> DeliveryTask.TaskStatus.PENDING;
                    case 1 -> DeliveryTask.TaskStatus.ASSIGNED;
                    case 2 -> DeliveryTask.TaskStatus.IN_PROGRESS;
                    default -> DeliveryTask.TaskStatus.COMPLETED;
                };
                task.setStatus(status);
                if (status != DeliveryTask.TaskStatus.PENDING) {
                    task.setCourierId(couriers.get(i % couriers.size()).getId());
                    task.setStartedAt(baseTime.plusMinutes(i * 3L + 10));
                }
                if (status == DeliveryTask.TaskStatus.COMPLETED) {
                    task.setCompletedAt(baseTime.plusMinutes(i * 3L + 60));
                    task.setActualDistance(task.getEstimatedDistance());
                    task.setActualTime(task.getEstimatedTime());
                }
                deliveryTaskRepository.save(task);
                created++;
            }
        }

        // 2) 悬赏任务（REWARD）- 只生成 PENDING/ASSIGNED/COMPLETED 的混合，且同步钱包冻结
        long existingReward = deliveryTaskRepository.findAll().stream()
            .filter(t -> t.getType() == DeliveryTask.TaskType.REWARD).count();
        if (existingReward >= rewardTaskTarget) return;

        int needReward = (int) (rewardTaskTarget - existingReward);
        int createdReward = 0;
        for (int i = 0; i < allPackages.size() && createdReward < needReward; i++) {
            Package pkg = allPackages.get(i);
            if (pkg.getStatus() != Package.PackageStatus.IN_STORAGE) continue;

            User publisher = students.get(i % students.size());
            if (!pkg.getStudentId().equals(publisher.getId())) continue;

            BigDecimal reward = BigDecimal.valueOf((i % 10) + 1).setScale(2); // 1~10 元

            // 冻结（简单做：直接调钱包表，避免依赖业务 service）
            UserWallet w = userWalletRepository.findByUserId(publisher.getId()).orElse(null);
            if (w == null) continue;
            if (w.getBalance() == null) w.setBalance(BigDecimal.ZERO);
            if (w.getFrozen() == null) w.setFrozen(BigDecimal.ZERO);
            if (w.getBalance().compareTo(reward) < 0) {
                w.setBalance(w.getBalance().add(new BigDecimal("500.00")));
            }
            w.setBalance(w.getBalance().subtract(reward));
            w.setFrozen(w.getFrozen().add(reward));
            userWalletRepository.save(w);

            DeliveryTask task = new DeliveryTask();
            task.setType(DeliveryTask.TaskType.REWARD);
            task.setPackageId(pkg.getId());
            task.setPublisherId(publisher.getId());
            task.setDestination(destinations[i % destinations.length]);
            task.setRewardAmount(reward);
            task.setPriority(2);
            task.setCreatedAt(baseTime.plusMinutes(i * 5L));

            DeliveryTask.TaskStatus status = switch (i % 3) {
                case 0 -> DeliveryTask.TaskStatus.PENDING;
                case 1 -> DeliveryTask.TaskStatus.ASSIGNED;
                default -> DeliveryTask.TaskStatus.COMPLETED;
            };
            task.setStatus(status);
            if (status != DeliveryTask.TaskStatus.PENDING) {
                // 让一部分悬赏有接单人（学生/快递员混合）
                User taker = (i % 2 == 0) ? students.get((i + 3) % students.size()) : couriers.get(i % couriers.size());
                if (!taker.getId().equals(publisher.getId())) {
                    task.setCourierId(taker.getId());
                    task.setStartedAt(baseTime.plusMinutes(i * 5L + 5));
                } else {
                    task.setStatus(DeliveryTask.TaskStatus.PENDING);
                }
            }
            if (task.getStatus() == DeliveryTask.TaskStatus.COMPLETED && task.getCourierId() != null) {
                task.setCompletedAt(baseTime.plusMinutes(i * 5L + 40));
                // 结算：把 publisher 冻结转给接单人余额
                UserWallet pubW = userWalletRepository.findByUserId(publisher.getId()).orElse(null);
                UserWallet takerW = userWalletRepository.findByUserId(task.getCourierId()).orElse(null);
                if (pubW != null && takerW != null && pubW.getFrozen().compareTo(reward) >= 0) {
                    pubW.setFrozen(pubW.getFrozen().subtract(reward));
                    takerW.setBalance((takerW.getBalance() == null ? BigDecimal.ZERO : takerW.getBalance()).add(reward));
                    userWalletRepository.save(pubW);
                    userWalletRepository.save(takerW);
                }
            }
            deliveryTaskRepository.save(task);
            createdReward++;
        }
    }
}
