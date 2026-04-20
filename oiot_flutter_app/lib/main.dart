import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';

void main() {
  runApp(const OIotApp());
}

class OIotApp extends StatelessWidget {
  const OIotApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'O-IoT Controller',
      theme: ThemeData(colorSchemeSeed: Colors.indigo, useMaterial3: true),
      home: const ControllerPage(),
    );
  }
}

class ControllerPage extends StatefulWidget {
  const ControllerPage({super.key});

  @override
  State<ControllerPage> createState() => _ControllerPageState();
}

class _ControllerPageState extends State<ControllerPage> {
  final OiotApi _api = OiotApi();
  List<DeviceDto> _devices = <DeviceDto>[];
  String _lastMessage = '백엔드 연결 확인 중...';
  bool _loading = true;
  bool _connected = false;

  @override
  void initState() {
    super.initState();
    _reload();
  }

  Future<void> _reload() async {
    setState(() {
      _loading = true;
    });
    try {
      final List<DeviceDto> devices = await _api.fetchDevices();
      if (!mounted) return;
      setState(() {
        _devices = devices;
        _connected = true;
        _lastMessage = '백엔드 연결 성공 (${devices.length}개 장치)';
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _connected = false;
        _lastMessage = '백엔드 연결 실패: $e';
      });
    } finally {
      if (mounted) {
        setState(() {
          _loading = false;
        });
      }
    }
  }

  Future<void> _togglePower(DeviceDto d, bool on) async {
    await _api.togglePower(d.id, on);
    await _reload();
  }

  Future<void> _simulateTick() async {
    await _api.simulate(1);
    await _reload();
  }

  Future<void> _applyPreset(String preset) async {
    await _api.applyScenario(preset);
    await _reload();
  }

  Future<void> _saveLog() async {
    final String path = await _api.saveLog();
    if (!mounted) return;
    setState(() {
      _lastMessage = '로그 저장: $path';
    });
  }

  Future<void> _setOption(DeviceDto d) async {
    final TextEditingController controller = TextEditingController(text: '${d.value}');
    final int? value = await showDialog<int>(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Text('${d.id} 값 설정'),
          content: TextField(
            controller: controller,
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(labelText: '값 입력'),
          ),
          actions: <Widget>[
            TextButton(onPressed: () => Navigator.pop(context), child: const Text('취소')),
            FilledButton(
              onPressed: () => Navigator.pop(context, int.tryParse(controller.text)),
              child: const Text('저장'),
            ),
          ],
        );
      },
    );
    if (value == null) return;
    await _api.setOption(d.id, value);
    await _reload();
  }

  Future<void> _addDeviceDialog() async {
    final TextEditingController nameController = TextEditingController(text: 'New Device');
    String selectedType = 'LIGHT';

    final bool? ok = await showDialog<bool>(
      context: context,
      builder: (BuildContext context) {
        return StatefulBuilder(
          builder: (BuildContext context, StateSetter setDialogState) {
            return AlertDialog(
              title: const Text('장치 추가'),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                children: <Widget>[
                  DropdownButton<String>(
                    value: selectedType,
                    items: const <String>['LIGHT', 'THERMOSTAT', 'SECURITY_SENSOR', 'AIR_PURIFIER', 'BLIND']
                        .map((String type) => DropdownMenuItem<String>(
                              value: type,
                              child: Text(type),
                            ))
                        .toList(),
                    onChanged: (String? value) {
                      if (value != null) {
                        setDialogState(() {
                          selectedType = value;
                        });
                      }
                    },
                  ),
                  TextField(
                    controller: nameController,
                    decoration: const InputDecoration(labelText: '장치 이름'),
                  ),
                ],
              ),
              actions: <Widget>[
                TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('취소')),
                FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text('추가')),
              ],
            );
          },
        );
      },
    );

    if (ok != true) return;
    await _api.addDevice(selectedType, nameController.text.trim());
    await _reload();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('O-IoT Flutter Controller')),
      body: Column(
        children: <Widget>[
          Padding(
            padding: const EdgeInsets.all(12),
            child: Wrap(
              spacing: 8,
              runSpacing: 8,
              children: <Widget>[
                FilledButton(
                  onPressed: _connected ? _simulateTick : null,
                  child: const Text('Tick'),
                ),
                FilledButton(
                  onPressed: _connected ? () => _applyPreset('HOME') : null,
                  child: const Text('HOME'),
                ),
                FilledButton(
                  onPressed: _connected ? () => _applyPreset('AWAY') : null,
                  child: const Text('AWAY'),
                ),
                FilledButton(
                  onPressed: _connected ? () => _applyPreset('NIGHT') : null,
                  child: const Text('NIGHT'),
                ),
                OutlinedButton(
                  onPressed: _connected ? _saveLog : null,
                  child: const Text('로그 저장'),
                ),
                OutlinedButton(onPressed: _reload, child: const Text('새로고침')),
                OutlinedButton(
                  onPressed: _connected ? _addDeviceDialog : null,
                  child: const Text('장치 추가'),
                ),
              ],
            ),
          ),
          const Divider(),
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : ListView.builder(
                    itemCount: _devices.length,
                    itemBuilder: (BuildContext context, int i) {
                      final DeviceDto d = _devices[i];
                      return Card(
                        margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                        child: ListTile(
                          title: Text('${d.id} · ${d.name}'),
                          subtitle: Text('${d.type} | 값: ${d.value}\n${d.summary}'),
                          isThreeLine: true,
                          trailing: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: <Widget>[
                              IconButton(
                                icon: const Icon(Icons.tune),
                                onPressed: _connected ? () => _setOption(d) : null,
                              ),
                              Switch(
                                value: d.powerOn,
                                onChanged: _connected ? (bool v) => _togglePower(d, v) : null,
                              ),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
          ),
          Padding(
            padding: const EdgeInsets.all(12),
            child: Align(
              alignment: Alignment.centerLeft,
              child: Text('상태: $_lastMessage'),
            ),
          ),
        ],
      ),
    );
  }
}

class DeviceDto {
  DeviceDto({
    required this.id,
    required this.name,
    required this.type,
    required this.powerOn,
    required this.value,
    required this.summary,
  });

  final String id;
  final String name;
  final String type;
  final bool powerOn;
  final int value;
  final String summary;

  factory DeviceDto.fromJson(Map<String, dynamic> json) {
    return DeviceDto(
      id: json['id'] as String,
      name: json['name'] as String,
      type: json['type'] as String,
      powerOn: json['powerOn'] as bool? ?? false,
      value: json['value'] as int? ?? 0,
      summary: json['summary'] as String? ?? '',
    );
  }
}

class OiotApi {
  static const String _base = 'http://localhost:8080/api';

  Future<List<DeviceDto>> fetchDevices() async {
    final http.Response res = await http.get(Uri.parse('$_base/devices'));
    if (res.statusCode != 200) {
      throw Exception('HTTP ${res.statusCode}');
    }
    final Map<String, dynamic> map = jsonDecode(res.body) as Map<String, dynamic>;
    final List<dynamic> list = map['devices'] as List<dynamic>;
    return list.map((dynamic e) => DeviceDto.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<void> togglePower(String id, bool on) async {
    await _post('/device/power?id=$id&on=$on');
  }

  Future<void> setOption(String id, int value) async {
    await _post('/device/option?id=$id&value=$value');
  }

  Future<void> simulate(int count) async {
    await _post('/simulate?count=$count');
  }

  Future<void> applyScenario(String preset) async {
    await _post('/scenario?preset=$preset');
  }

  Future<void> addDevice(String type, String name) async {
    final String encodedName = Uri.encodeComponent(name);
    await _post('/device/add?type=$type&name=$encodedName');
  }

  Future<String> saveLog() async {
    final http.Response res = await _post('/log/save');
    final Map<String, dynamic> map = jsonDecode(res.body) as Map<String, dynamic>;
    return map['path'] as String? ?? 'saved';
  }

  Future<http.Response> _post(String path) async {
    final http.Response res = await http.post(Uri.parse('$_base$path'));
    if (res.statusCode != 200) {
      throw Exception('HTTP ${res.statusCode}');
    }
    return res;
  }
}
