package oiot.backend;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

public class BackendCliServer {
    private static final int PORT = 8080;

    private final SmartHomeController controller = new SmartHomeController();
    private final ScenarioEngine scenarioEngine = new ScenarioEngine(controller);
    private final StatusLogger logger = new StatusLogger(Path.of("logs"));
    private final Map<String, Integer> idCounter = new HashMap<>();
    private HttpServer server;

    public static void main(String[] args) throws Exception {
        new BackendCliServer().run();
    }

    private void run() throws Exception {
        seedMockDevices();
        startHttpServer();
        if (System.console() == null) {
            keepServerAlive();
            return;
        }
        runCliLoop();
    }

    private void seedMockDevices() {
        controller.registerDevice(new Light("L-01", "Living Room Light"));
        controller.registerDevice(new Thermostat("T-01", "Main Thermostat", 23.0));
        controller.registerDevice(new SecuritySensor("S-01", "Front Door Sensor"));
        controller.registerDevice(new AirPurifier("A-01", "Bedroom Air Purifier"));
        controller.registerDevice(new SmartBlind("B-01", "Living Room Blind"));

        idCounter.put("L", 1);
        idCounter.put("T", 1);
        idCounter.put("S", 1);
        idCounter.put("A", 1);
        idCounter.put("B", 1);
    }

    private void startHttpServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/health", this::handleHealth);
        server.createContext("/api/devices", this::handleDevices);
        server.createContext("/api/device/power", this::handleDevicePower);
        server.createContext("/api/device/option", this::handleDeviceOption);
        server.createContext("/api/device/add", this::handleDeviceAdd);
        server.createContext("/api/scenario", this::handleScenario);
        server.createContext("/api/simulate", this::handleSimulate);
        server.createContext("/api/log/save", this::handleLogSave);
        server.start();
        System.out.println("[Backend] HTTP server started at http://localhost:" + PORT);
        printHelp();
    }

    private void runCliLoop() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("backend> ");
            String line = scanner.nextLine().trim();
            if (line.isBlank()) {
                continue;
            }

            String[] parts = line.split("\\s+");
            String command = parts[0].toLowerCase();

            try {
                switch (command) {
                    case "help":
                        printHelp();
                        break;
                    case "status":
                        System.out.println(controller.getSystemStatus());
                        break;
                    case "tick":
                        int count = parts.length >= 2 ? Integer.parseInt(parts[1]) : 1;
                        for (int i = 0; i < count; i++) {
                            controller.simulateAllDevices();
                        }
                        System.out.println("Tick " + count + "회 실행 완료");
                        break;
                    case "on":
                        setPower(parts, true);
                        break;
                    case "off":
                        setPower(parts, false);
                        break;
                    case "set":
                        setOption(parts);
                        break;
                    case "scenario":
                        applyScenario(parts);
                        break;
                    case "add":
                        addDeviceFromCli(parts);
                        break;
                    case "log":
                        System.out.println("Saved: " + logger.saveSnapshot(controller.getSystemStatus()).toAbsolutePath());
                        break;
                    case "quit":
                    case "exit":
                        server.stop(0);
                        System.out.println("Backend 종료");
                        return;
                    default:
                        System.out.println("알 수 없는 명령어입니다. help 입력");
                }
            } catch (Exception e) {
                System.out.println("명령 처리 실패: " + e.getMessage());
            }
        }
    }

    private void keepServerAlive() {
        System.out.println("[Backend] Console 입력이 없어 server-only 모드로 대기합니다.");
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                server.stop(0);
                return;
            }
        }
    }

    private void setPower(String[] parts, boolean on) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("사용법: on|off <deviceId>");
        }
        SmartDevice d = getDevice(parts[1]);
        if (on) {
            d.turnOn();
        } else {
            d.turnOff();
        }
        System.out.println(d.getName() + " -> " + (on ? "ON" : "OFF"));
    }

    private void setOption(String[] parts) {
        if (parts.length < 3) {
            throw new IllegalArgumentException("사용법: set <deviceId> <value>");
        }
        SmartDevice d = getDevice(parts[1]);
        int value = Integer.parseInt(parts[2]);
        applyOption(d, value);
        System.out.println("설정 완료: " + d.getStatusSummary());
    }

    private void applyScenario(String[] parts) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("사용법: scenario <HOME|AWAY|NIGHT>");
        }
        ScenarioPreset preset = ScenarioPreset.valueOf(parts[1].toUpperCase());
        scenarioEngine.apply(preset);
        System.out.println("프리셋 적용: " + preset);
    }

    private void addDeviceFromCli(String[] parts) {
        if (parts.length < 3) {
            throw new IllegalArgumentException("사용법: add <LIGHT|THERMOSTAT|SECURITY_SENSOR|AIR_PURIFIER|BLIND> <name>");
        }
        String type = parts[1].toUpperCase();
        String name = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
        SmartDevice d = addDevice(type, name);
        System.out.println("장치 추가: " + d.getStatusSummary());
    }

    private SmartDevice addDevice(String type, String name) {
        SmartDevice d;
        switch (type) {
            case "LIGHT":
                d = new Light(nextId("L"), name);
                break;
            case "THERMOSTAT":
                d = new Thermostat(nextId("T"), name, 22.0);
                break;
            case "SECURITY_SENSOR":
                d = new SecuritySensor(nextId("S"), name);
                break;
            case "AIR_PURIFIER":
                d = new AirPurifier(nextId("A"), name);
                break;
            case "BLIND":
                d = new SmartBlind(nextId("B"), name);
                break;
            default:
                throw new IllegalArgumentException("지원하지 않는 타입");
        }
        controller.registerDevice(d);
        return d;
    }

    private String nextId(String prefix) {
        int next = idCounter.getOrDefault(prefix, 0) + 1;
        idCounter.put(prefix, next);
        return String.format("%s-%02d", prefix, next);
    }

    private SmartDevice getDevice(String id) {
        return controller.findDeviceById(id)
                .orElseThrow(() -> new IllegalArgumentException("장치 없음: " + id));
    }

    private void applyOption(SmartDevice device, int value) {
        if (device instanceof Light light) {
            light.setBrightness(value);
        } else if (device instanceof Thermostat thermostat) {
            thermostat.setTargetTemp(value);
        } else if (device instanceof SecuritySensor sensor) {
            sensor.setSensitivity(value);
        } else if (device instanceof AirPurifier purifier) {
            purifier.setFanLevel(value);
        } else if (device instanceof SmartBlind blind) {
            blind.setOpenness(value);
        }
    }

    private void printHelp() {
        System.out.println("==== CLI Commands ====");
        System.out.println("help");
        System.out.println("status");
        System.out.println("tick [n]");
        System.out.println("on <deviceId>");
        System.out.println("off <deviceId>");
        System.out.println("set <deviceId> <value>");
        System.out.println("scenario <HOME|AWAY|NIGHT>");
        System.out.println("add <LIGHT|THERMOSTAT|SECURITY_SENSOR|AIR_PURIFIER|BLIND> <name>");
        System.out.println("log");
        System.out.println("quit");
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        writeJson(exchange, 200, "{\"ok\":true}");
    }

    private void handleDevices(HttpExchange exchange) throws IOException {
        if (!isGet(exchange)) {
            writeJson(exchange, 405, "{\"error\":\"GET only\"}");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{\"devices\":[");
        List<SmartDevice> devices = controller.getDevices();
        for (int i = 0; i < devices.size(); i++) {
            SmartDevice d = devices.get(i);
            sb.append(deviceToJson(d));
            if (i < devices.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]}");
        writeJson(exchange, 200, sb.toString());
    }

    private void handleDevicePower(HttpExchange exchange) throws IOException {
        if (!isPost(exchange)) {
            writeJson(exchange, 405, "{\"error\":\"POST only\"}");
            return;
        }
        Map<String, String> q = parseQuery(exchange.getRequestURI());
        SmartDevice d = getDevice(q.getOrDefault("id", ""));
        boolean on = Boolean.parseBoolean(q.getOrDefault("on", "false"));
        if (on) {
            d.turnOn();
        } else {
            d.turnOff();
        }
        writeJson(exchange, 200, "{\"ok\":true}");
    }

    private void handleDeviceOption(HttpExchange exchange) throws IOException {
        if (!isPost(exchange)) {
            writeJson(exchange, 405, "{\"error\":\"POST only\"}");
            return;
        }
        Map<String, String> q = parseQuery(exchange.getRequestURI());
        SmartDevice d = getDevice(q.getOrDefault("id", ""));
        int value = Integer.parseInt(q.getOrDefault("value", "0"));
        applyOption(d, value);
        writeJson(exchange, 200, "{\"ok\":true}");
    }

    private void handleDeviceAdd(HttpExchange exchange) throws IOException {
        if (!isPost(exchange)) {
            writeJson(exchange, 405, "{\"error\":\"POST only\"}");
            return;
        }
        Map<String, String> q = parseQuery(exchange.getRequestURI());
        String type = q.getOrDefault("type", "");
        String name = q.getOrDefault("name", "New Device");
        SmartDevice d = addDevice(type.toUpperCase(), name);
        writeJson(exchange, 200, "{\"ok\":true,\"id\":\"" + d.getId() + "\"}");
    }

    private void handleScenario(HttpExchange exchange) throws IOException {
        if (!isPost(exchange)) {
            writeJson(exchange, 405, "{\"error\":\"POST only\"}");
            return;
        }
        Map<String, String> q = parseQuery(exchange.getRequestURI());
        ScenarioPreset preset = ScenarioPreset.valueOf(q.getOrDefault("preset", "HOME").toUpperCase());
        scenarioEngine.apply(preset);
        writeJson(exchange, 200, "{\"ok\":true}");
    }

    private void handleSimulate(HttpExchange exchange) throws IOException {
        if (!isPost(exchange)) {
            writeJson(exchange, 405, "{\"error\":\"POST only\"}");
            return;
        }
        Map<String, String> q = parseQuery(exchange.getRequestURI());
        int count = Integer.parseInt(q.getOrDefault("count", "1"));
        for (int i = 0; i < count; i++) {
            controller.simulateAllDevices();
        }
        writeJson(exchange, 200, "{\"ok\":true}");
    }

    private void handleLogSave(HttpExchange exchange) throws IOException {
        if (!isPost(exchange)) {
            writeJson(exchange, 405, "{\"error\":\"POST only\"}");
            return;
        }
        String path = logger.saveSnapshot(controller.getSystemStatus()).toAbsolutePath().toString();
        writeJson(exchange, 200, "{\"ok\":true,\"path\":\"" + escape(path) + "\"}");
    }

    private boolean isGet(HttpExchange exchange) {
        return "GET".equalsIgnoreCase(exchange.getRequestMethod());
    }

    private boolean isPost(HttpExchange exchange) {
        return "POST".equalsIgnoreCase(exchange.getRequestMethod());
    }

    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> map = new HashMap<>();
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) {
            return map;
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = urlDecode(kv[0]);
            String val = kv.length > 1 ? urlDecode(kv[1]) : "";
            map.put(key, val);
        }
        return map;
    }

    private String urlDecode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    private String deviceToJson(SmartDevice d) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(escape(d.getId())).append("\",");
        sb.append("\"name\":\"").append(escape(d.getName())).append("\",");
        sb.append("\"type\":\"").append(escape(d.getType().name())).append("\",");
        sb.append("\"powerOn\":").append(d.isOn()).append(",");
        sb.append("\"summary\":\"").append(escape(d.getStatusSummary())).append("\"");
        if (d instanceof Light light) {
            sb.append(",\"value\":").append(light.getBrightness());
        } else if (d instanceof Thermostat thermostat) {
            sb.append(",\"value\":").append((int) thermostat.getTargetTemp());
        } else if (d instanceof SecuritySensor sensor) {
            sb.append(",\"value\":").append(sensor.getSensitivity());
            sb.append(",\"motionDetected\":").append(sensor.isMotionDetected());
        } else if (d instanceof AirPurifier purifier) {
            sb.append(",\"value\":").append(purifier.getFanLevel());
        } else if (d instanceof SmartBlind blind) {
            sb.append(",\"value\":").append(blind.getOpenness());
        }
        sb.append("}");
        return sb.toString();
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void writeJson(HttpExchange exchange, int code, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(code, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }
}
