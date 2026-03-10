package com.campus.express.config;

import com.campus.express.entity.DeliveryTask;
import com.campus.express.entity.Package;
import com.campus.express.entity.User;
import com.campus.express.entity.VirtualShelf;
import com.campus.express.repository.DeliveryTaskRepository;
import com.campus.express.repository.PackageRepository;
import com.campus.express.repository.UserRepository;
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
    private final PasswordEncoder passwordEncoder;

    private static final String DEMO_PASSWORD = "123456";

    @Override
    public void run(String... args) {
        initAdmin();
        initShelves();
        initUsers();
        if (packageRepository.count() == 0) {
            initPackages();
        }
        if (deliveryTaskRepository.count() == 0) {
            initDeliveryTasks();
        }
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
            userRepository.save(courier);
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
            userRepository.save(student);
        }
    }

    private void initPackages() {
        List<User> students = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.UserRole.STUDENT).toList();
        List<String> shelfCodes = shelfRepository.findAll().stream()
                .map(VirtualShelf::getShelfCode).limit(30).toList();
        if (students.isEmpty() || shelfCodes.isEmpty()) return;

        Package.PackageSize[] sizes = Package.PackageSize.values();
        Package.PackageStatus[] statuses = Package.PackageStatus.values();
        LocalDateTime baseTime = LocalDateTime.now().minusDays(7);

        for (int i = 1; i <= 35; i++) {
            Package pkg = new Package();
            pkg.setTrackingNumber("SF" + String.format("%012d", 100000000000L + i));
            pkg.setStudentId(students.get(i % students.size()).getId());
            pkg.setSize(sizes[i % sizes.length]);
            pkg.setShelfCode(shelfCodes.get(i % shelfCodes.size()));
            pkg.setStorageTime(baseTime.plusDays(i % 5));

            int statusIdx = i % 10;
            if (statusIdx < 4) pkg.setStatus(Package.PackageStatus.IN_STORAGE);
            else if (statusIdx < 6) pkg.setStatus(Package.PackageStatus.OUT_FOR_DELIVERY);
            else if (statusIdx < 8) pkg.setStatus(Package.PackageStatus.COMPLETED);
            else pkg.setStatus(Package.PackageStatus.DELIVERED);

            if (pkg.getStatus() != Package.PackageStatus.IN_STORAGE) {
                pkg.setEstimatedDeliveryTime(baseTime.plusDays(i % 5 + 1));
            }
            if (pkg.getStatus() == Package.PackageStatus.COMPLETED || pkg.getStatus() == Package.PackageStatus.DELIVERED) {
                pkg.setActualDeliveryTime(baseTime.plusDays(i % 5 + 2));
            }
            packageRepository.save(pkg);
        }
    }

    private void initDeliveryTasks() {
        List<Package> packages = packageRepository.findAll();
        List<User> couriers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == User.UserRole.COURIER).toList();
        if (packages.isEmpty() || couriers.isEmpty()) return;

        String[] destinations = {"DORM_1", "DORM_2", "DORM_3", "CAFETERIA", "STATION_1"};
        DeliveryTask.TaskStatus[] taskStatuses = {DeliveryTask.TaskStatus.PENDING, DeliveryTask.TaskStatus.ASSIGNED,
                DeliveryTask.TaskStatus.IN_PROGRESS, DeliveryTask.TaskStatus.COMPLETED, DeliveryTask.TaskStatus.PENDING};
        LocalDateTime baseTime = LocalDateTime.now().minusDays(5);

        int count = Math.min(25, packages.size());
        for (int i = 0; i < count; i++) {
            Package pkg = packages.get(i);
            DeliveryTask task = new DeliveryTask();
            task.setPackageId(pkg.getId());
            task.setDestination(destinations[i % destinations.length]);
            task.setPathJson("[{\"x\":0,\"y\":0},{\"x\":3,\"y\":3}]");
            task.setEstimatedDistance(BigDecimal.valueOf(150 + i * 20));
            task.setEstimatedTime(BigDecimal.valueOf(5 + i % 10));
            task.setPriority(1 + (i % 3));
            task.setStatus(taskStatuses[i % taskStatuses.length]);
            task.setCreatedAt(baseTime.plusHours(i * 2));

            if (task.getStatus() != DeliveryTask.TaskStatus.PENDING) {
                task.setCourierId(couriers.get(i % couriers.size()).getId());
            }
            if (task.getStatus() == DeliveryTask.TaskStatus.COMPLETED) {
                task.setCompletedAt(baseTime.plusHours(i * 2 + 1));
                task.setActualDistance(task.getEstimatedDistance());
                task.setActualTime(task.getEstimatedTime());
            }
            deliveryTaskRepository.save(task);
        }
    }
}
