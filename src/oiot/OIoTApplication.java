package oiot;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import oiot.core.SmartDevice;
import oiot.devices.AirPurifier;
import oiot.devices.Light;
import oiot.devices.SecuritySensor;
import oiot.devices.SmartBlind;
import oiot.devices.Thermostat;
import oiot.scenario.ScenarioEngine;
import oiot.scenario.ScenarioPreset;
import oiot.system.SmartHomeController;
import oiot.system.StatusLogger;

public class OIoTApplication {
    private final SmartHomeController controller = new SmartHomeController();
    private final Scanner scanner = new Scanner(System.in);
    private final ScenarioEngine scenarioEngine = new ScenarioEngine(controller);
    private final StatusLogger statusLogger = new StatusLogger(Path.of("logs"));
    private final Map<String, Integer> idCounter = new HashMap<>();

    public static void main(String[] args) {
        new OIoTApplication().run();
    }

    private void run() {
        seedMockDevices();
        printBanner();

        boolean running = true;
        while (running) {
            printMenu();
            String command = scanner.nextLine().trim();
            switch (command) {
                case "1":
                    printStatus();
                    break;
                case "2":
                    powerControl(true);
                    break;
                case "3":
                    powerControl(false);
                    break;
                case "4":
                    setDeviceOption();
                    break;
                case "5":
                    runSimulationTick();
                    break;
                case "6":
                    runMultipleTicks();
                    break;
                case "7":
                    addNewDevice();
                    break;
                case "8":
                    applyPresetScenario();
                    break;
                case "9":
                    saveStatusLog();
                    break;
                case "0":
                    running = false;
                    break;
                default:
                    System.out.println("알 수 없는 메뉴입니다.");
            }
        }
        System.out.println("O-IoT 시스템을 종료합니다.");
    }

    private void seedMockDevices() {
        controller.registerDevice(new Light("L-01", "Living Room Light"));
        controller.registerDevice(new Light("L-02", "Kitchen Light"));
        controller.registerDevice(new Thermostat("T-01", "Main Thermostat", 23.0));
        controller.registerDevice(new SecuritySensor("S-01", "Front Door Sensor"));
        controller.registerDevice(new AirPurifier("A-01", "Bedroom Air Purifier"));
        controller.registerDevice(new SmartBlind("B-01", "Living Room Blind"));

        idCounter.put("L", 2);
        idCounter.put("T", 1);
        idCounter.put("S", 1);
        idCounter.put("A", 1);
        idCounter.put("B", 1);
    }

    private void printBanner() {
        System.out.println("========================================");
        System.out.println(" O-IoT : Smart Environment Controller");
        System.out.println(" (Console Simulation / Java OOP Demo)");
        System.out.println("========================================");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("[메뉴]");
        System.out.println("1. 전체 장치 상태 조회");
        System.out.println("2. 장치 전원 ON");
        System.out.println("3. 장치 전원 OFF");
        System.out.println("4. 장치 옵션 변경(밝기/목표온도/민감도)");
        System.out.println("5. 시뮬레이션 1 Tick 실행");
        System.out.println("6. 시뮬레이션 N Tick 실행");
        System.out.println("7. 장치 추가");
        System.out.println("8. 시나리오 프리셋 적용(HOME/AWAY/NIGHT)");
        System.out.println("9. 상태 로그 파일 저장");
        System.out.println("0. 종료");
        System.out.print("선택 > ");
    }

    private void printStatus() {
        System.out.println(controller.getSystemStatus());
    }

    private void powerControl(boolean turnOn) {
        SmartDevice device = askDeviceById();
        if (device == null) {
            return;
        }
        if (turnOn) {
            device.turnOn();
            System.out.println(device.getName() + " 전원을 ON 했습니다.");
        } else {
            device.turnOff();
            System.out.println(device.getName() + " 전원을 OFF 했습니다.");
        }
    }

    private void setDeviceOption() {
        SmartDevice device = askDeviceById();
        if (device == null) {
            return;
        }

        try {
            if (device instanceof Light light) {
                System.out.print("밝기(0~100) 입력 > ");
                int value = Integer.parseInt(scanner.nextLine().trim());
                light.setBrightness(value);
                System.out.println("밝기를 " + value + "%로 설정했습니다.");
            } else if (device instanceof Thermostat thermostat) {
                System.out.print("목표 온도(10~35) 입력 > ");
                double value = Double.parseDouble(scanner.nextLine().trim());
                thermostat.setTargetTemp(value);
                System.out.printf("목표 온도를 %.1fC로 설정했습니다.%n", value);
            } else if (device instanceof SecuritySensor sensor) {
                System.out.print("민감도(1~10) 입력 > ");
                int value = Integer.parseInt(scanner.nextLine().trim());
                sensor.setSensitivity(value);
                System.out.println("민감도를 " + value + "로 설정했습니다.");
            } else if (device instanceof AirPurifier purifier) {
                System.out.print("팬 단계(1~5) 입력 > ");
                int value = Integer.parseInt(scanner.nextLine().trim());
                purifier.setFanLevel(value);
                System.out.println("팬 단계를 " + value + "로 설정했습니다.");
            } else if (device instanceof SmartBlind blind) {
                System.out.print("개방도(0~100) 입력 > ");
                int value = Integer.parseInt(scanner.nextLine().trim());
                blind.setOpenness(value);
                System.out.println("개방도를 " + value + "%로 설정했습니다.");
            } else {
                System.out.println("해당 장치는 설정 가능한 옵션이 없습니다.");
            }
        } catch (NumberFormatException e) {
            System.out.println("숫자 형식이 올바르지 않습니다.");
        } catch (IllegalArgumentException e) {
            System.out.println("입력 오류: " + e.getMessage());
        }
    }

    private void runSimulationTick() {
        controller.simulateAllDevices();
        System.out.println("시뮬레이션 Tick 1회 완료.");
        printStatus();
    }

    private void runMultipleTicks() {
        System.out.print("Tick 횟수 입력 > ");
        try {
            int count = Integer.parseInt(scanner.nextLine().trim());
            if (count <= 0) {
                System.out.println("1 이상의 값을 입력하세요.");
                return;
            }
            for (int i = 0; i < count; i++) {
                controller.simulateAllDevices();
            }
            System.out.println("시뮬레이션 Tick " + count + "회 완료.");
            printStatus();
        } catch (NumberFormatException e) {
            System.out.println("숫자 형식이 올바르지 않습니다.");
        }
    }

    private void addNewDevice() {
        System.out.println("추가할 장치 유형을 선택하세요:");
        System.out.println("1) Light");
        System.out.println("2) Thermostat");
        System.out.println("3) SecuritySensor");
        System.out.println("4) AirPurifier");
        System.out.println("5) SmartBlind");
        System.out.print("선택 > ");
        String type = scanner.nextLine().trim();

        System.out.print("장치 이름 입력 > ");
        String name = scanner.nextLine().trim();
        if (name.isBlank()) {
            System.out.println("장치 이름은 비어 있을 수 없습니다.");
            return;
        }

        SmartDevice newDevice;
        switch (type) {
            case "1":
                newDevice = new Light(nextId("L"), name);
                break;
            case "2":
                newDevice = new Thermostat(nextId("T"), name, 22.0);
                break;
            case "3":
                newDevice = new SecuritySensor(nextId("S"), name);
                break;
            case "4":
                newDevice = new AirPurifier(nextId("A"), name);
                break;
            case "5":
                newDevice = new SmartBlind(nextId("B"), name);
                break;
            default:
                System.out.println("지원하지 않는 장치 유형입니다.");
                return;
        }
        controller.registerDevice(newDevice);
        System.out.println("장치가 추가되었습니다: " + newDevice.getStatusSummary());
    }

    private String nextId(String prefix) {
        int next = idCounter.getOrDefault(prefix, 0) + 1;
        idCounter.put(prefix, next);
        return String.format("%s-%02d", prefix, next);
    }

    private void applyPresetScenario() {
        System.out.println("프리셋 선택: 1) HOME 2) AWAY 3) NIGHT");
        System.out.print("선택 > ");
        String input = scanner.nextLine().trim();
        ScenarioPreset preset;
        switch (input) {
            case "1":
                preset = ScenarioPreset.HOME;
                break;
            case "2":
                preset = ScenarioPreset.AWAY;
                break;
            case "3":
                preset = ScenarioPreset.NIGHT;
                break;
            default:
                System.out.println("유효하지 않은 프리셋입니다.");
                return;
        }
        scenarioEngine.apply(preset);
        System.out.println("프리셋 적용 완료: " + preset);
        printStatus();
    }

    private void saveStatusLog() {
        String status = controller.getSystemStatus();
        Path saved = statusLogger.saveSnapshot(status);
        System.out.println("상태 로그 저장 완료: " + saved.toAbsolutePath());
    }

    private SmartDevice askDeviceById() {
        printStatus();
        System.out.print("장치 ID 입력 > ");
        String id = scanner.nextLine().trim();
        return controller.findDeviceById(id).orElseGet(() -> {
            System.out.println("해당 ID의 장치를 찾을 수 없습니다.");
            return null;
        });
    }
}
