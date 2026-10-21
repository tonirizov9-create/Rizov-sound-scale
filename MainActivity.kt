package com.rizov.soundscale

import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiInputPort
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF0B0E14)
private val Panel = Color(0xFF151A23)
private val Panel2 = Color(0xFF202734)
private val Gold = Color(0xFFD8A650)
private val Muted = Color(0xFF9AA4B4)
private val White = Color(0xFFF5F6F8)

class MainActivity : ComponentActivity() {
    private var midiManager: MidiManager? = null
    private var openedDevice: MidiDevice? = null
    private var outputPort: MidiInputPort? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        midiManager = getSystemService(MIDI_SERVICE) as? MidiManager
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Gold, onPrimary = Bg, background = Bg, surface = Panel,
                onBackground = White, onSurface = White, secondary = Gold
            )) {
                RizovApp(
                    listMidiDevices = { midiManager?.devices?.toList() ?: emptyList() },
                    connect = { info, onResult -> connectMidi(info, onResult) },
                    disconnect = { disconnectMidi() }
                )
            }
        }
    }

    private fun connectMidi(info: MidiDeviceInfo, onResult: (String) -> Unit) {
        val manager = midiManager
        if (manager == null) { onResult("Android MIDI не е наличен на това устройство."); return }
        manager.openDevice(info, { device ->
            if (device == null) { onResult("Неуспешно отваряне на MIDI устройството."); return@openDevice }
            openedDevice?.close()
            openedDevice = device
            val portInfo = info.ports.firstOrNull { it.type == MidiDeviceInfo.PortInfo.TYPE_INPUT }
            if (portInfo == null) {
                onResult("Устройството е отворено, но няма MIDI входен порт за изпращане към инструмента.")
                return@openDevice
            }
            outputPort = device.openInputPort(portInfo.portNumber)
            onResult(if (outputPort != null) "Свързано: ${info.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "USB-MIDI"}" else "Устройството е открито, но портът не се отвори.")
        }, null)
    }

    private fun disconnectMidi() {
        outputPort?.close(); outputPort = null
        openedDevice?.close(); openedDevice = null
    }

    override fun onDestroy() { disconnectMidi(); super.onDestroy() }
}

@Composable
private fun RizovApp(
    listMidiDevices: () -> List<MidiDeviceInfo>,
    connect: (MidiDeviceInfo, (String) -> Unit) -> Unit,
    disconnect: () -> Unit
) {
    var page by remember { mutableStateOf("Scales") }
    var scaleMode by remember { mutableStateOf("Октава · 12 тона") }
    var transpose by remember { mutableIntStateOf(0) }
    var connected by remember { mutableStateOf(false) }
    var connectionText by remember { mutableStateOf("Не е свързано") }
    var devices by remember { mutableStateOf(emptyList<MidiDeviceInfo>()) }
    var showDevices by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf("Равномерна") }
    var notes by remember { mutableStateOf(List(128) { 0 }) }
    var selectedNoteCount by remember { mutableIntStateOf(12) }
    var selectedNote by remember { mutableIntStateOf(0) }
    var thirdsOn by remember { mutableStateOf(false) }
    var thirdType by remember { mutableStateOf("Голяма терца") }

    Surface(Modifier.fillMaxSize(), color = Bg) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).background(Gold, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Text("RS", color = Bg, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("RIZOV SOUND", color = White, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("S C A L E", color = Gold, fontSize = 11.sp, letterSpacing = 3.sp)
                }
                Box(Modifier.background(if (connected) Color(0xFF173B2B) else Panel2, CircleShape).clickable {
                    devices = listMidiDevices(); showDevices = true
                }.padding(horizontal = 11.dp, vertical = 8.dp)) {
                    Text(if (connected) "MIDI ●" else "MIDI +", color = if (connected) Color(0xFF78D9A6) else Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                NavChip("Scales", "Скали", page == "Scales") { page = "Scales" }
                NavChip("Transpose", "Транспон.", page == "Transpose") { page = "Transpose" }
                NavChip("Thirds", "Терци", page == "Thirds") { page = "Thirds" }
                NavChip("Presets", "Пресети", page == "Presets") { page = "Presets" }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ИНСТРУМЕНТ", color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp)
                    Text("KORG M3", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(connectionText, color = if (connected) Color(0xFF78D9A6) else Muted, fontSize = 12.sp)
                }
                OutlinedButton(onClick = { if (connected) { disconnect(); connected = false; connectionText = "Не е свързано" } else { devices = listMidiDevices(); showDevices = true } }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)) {
                    Text(if (connected) "Разкачи" else "Свържи")
                }
            }
            Spacer(Modifier.height(12.dp))
            when (page) {
                "Scales" -> ScalePage(
                    mode = scaleMode, setMode = { scaleMode = it; selectedNoteCount = if (it.startsWith("Октава")) 12 else 128 },
                    preset = selectedPreset, setPreset = { selectedPreset = it; notes = presetValues(it) },
                    notes = notes, setNotes = { notes = it }, count = selectedNoteCount,
                    selectedNote = selectedNote, setSelectedNote = { selectedNote = it }
                )
                "Transpose" -> TransposePage(transpose, { transpose = (transpose + it).coerceIn(-12, 12) }, { transpose = 0 })
                "Thirds" -> ThirdsPage(thirdsOn, { thirdsOn = it }, thirdType, { thirdType = it })
                else -> PresetsPage(selectedPreset, { selectedPreset = it; notes = presetValues(it); page = "Scales" })
            }
            Spacer(Modifier.height(10.dp))
            Text("ВАЖНО: настройките се редактират локално. Директното записване на скали в M3 се активира след потвърждаване на SysEx параметрите.", color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
            Spacer(Modifier.height(10.dp))
        }
    }

    if (showDevices) {
        AlertDialog(onDismissRequest = { showDevices = false }, containerColor = Panel,
            title = { Text("USB-MIDI устройства", color = White) },
            text = {
                if (devices.isEmpty()) Text("Не са открити MIDI устройства. Свържи USB-MIDI адаптера чрез OTG и натисни отново Свържи.", color = Muted)
                else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    devices.forEach { info ->
                        val name = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "MIDI устройство"
                        Text(name, color = White, modifier = Modifier.fillMaxWidth().clickable {
                            connect(info) { result -> connectionText = result; connected = result.startsWith("Свързано:") }
                            showDevices = false
                        }.padding(8.dp))
                    }
                }
            }, confirmButton = { TextButton(onClick = { devices = listMidiDevices() }) { Text("Обнови", color = Gold) } },
            dismissButton = { TextButton(onClick = { showDevices = false }) { Text("Затвори", color = Muted) } })
    }
}

@Composable
private fun RowScope.NavChip(id: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.weight(1f).background(if (selected) Gold else Panel, RoundedCornerShape(11.dp)).clickable(onClick = onClick).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (selected) Bg else Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ColumnScope.ScalePage(
    mode: String, setMode: (String) -> Unit, preset: String, setPreset: (String) -> Unit,
    notes: List<Int>, setNotes: (List<Int>) -> Unit, count: Int, selectedNote: Int, setSelectedNote: (Int) -> Unit
) {
    Column(Modifier.weight(1f)) {
        Text("SCALE STUDIO", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Настрой всеки тон с точност до 1 цент", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Октава · 12 тона", "Всички · 128").forEach { modeName ->
                Box(Modifier.weight(1f).background(if (mode == modeName) Panel2 else Panel, RoundedCornerShape(12.dp)).clickable { setMode(modeName) }.padding(12.dp), contentAlignment = Alignment.Center) {
                    Text(modeName, color = if (mode == modeName) Gold else Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Равномерна", "Балканска", "Ориенталска").forEach { p ->
                Box(Modifier.weight(1f).background(if (preset == p) Color(0xFF342A19) else Panel, RoundedCornerShape(10.dp)).clickable { setPreset(p) }.padding(vertical = 10.dp, horizontal = 3.dp), contentAlignment = Alignment.Center) {
                    Text(p, color = if (preset == p) Gold else Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (count == 12) {
            Column(Modifier.weight(1f).background(Panel, RoundedCornerShape(16.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                val names = listOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
                names.forEachIndexed { i, name ->
                    NoteRow(name, notes[i], { value -> setNotes(notes.toMutableList().also { it[i] = value }) }, i == selectedNote, { setSelectedNote(i) })
                }
            }
        } else {
            Column(Modifier.weight(1f)) {
                Text("Избери MIDI нота за редакция", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 24, 48, 60, 72, 96, 120).forEach { start ->
                        Box(Modifier.background(if (selectedNote in start until (start + 12).coerceAtMost(128)) Gold else Panel2, RoundedCornerShape(8.dp)).clickable { setSelectedNote(start) }.padding(8.dp)) {
                            Text("${start}-${(start + 11).coerceAtMost(127)}", color = if (selectedNote in start until (start + 12).coerceAtMost(128)) Bg else White, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.weight(1f).background(Panel, RoundedCornerShape(16.dp)).padding(10.dp)) {
                    items((selectedNote / 12 * 12 until (selectedNote / 12 * 12 + 12).coerceAtMost(128)).toList()) { i ->
                        NoteRow("MIDI $i", notes[i], { value -> setNotes(notes.toMutableList().also { it[i] = value }) }, i == selectedNote, { setSelectedNote(i) })
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { setNotes(List(128) { 0 }) }, modifier = Modifier.weight(1f)) { Text("Нулирай", color = Muted) }
            Button(onClick = { /* Hardware write intentionally disabled pending verified M3 SysEx scale addresses. */ }, enabled = false, modifier = Modifier.weight(1f)) { Text("Изпрати към M3") }
        }
    }
}

@Composable
private fun NoteRow(name: String, value: Int, onValue: (Int) -> Unit, selected: Boolean, onSelect: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(if (selected) Color(0xFF242B36) else Color.Transparent, RoundedCornerShape(9.dp)).clickable(onClick = onSelect).padding(horizontal = 8.dp, vertical = 3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, color = if (selected) Gold else White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(48.dp))
            Slider(value = value.toFloat(), onValueChange = { onValue(it.toInt()) }, valueRange = -99f..99f, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = Panel2))
            Text("${if (value > 0) "+" else ""}$value¢", color = if (value == 0) Muted else Gold, fontSize = 11.sp, modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
private fun ColumnScope.TransposePage(value: Int, change: (Int) -> Unit, reset: () -> Unit) {
    Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("TRANSPOSE CONTROL", color = White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text("Управление на транспонирането", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(30.dp))
        Box(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(24.dp)).padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ПОЛУТОНОВЕ", color = Muted, fontSize = 11.sp, letterSpacing = 2.sp)
                Text("${if (value > 0) "+" else ""}$value", color = Gold, fontSize = 64.sp, fontWeight = FontWeight.Black)
                Text(transposeLabel(value), color = White, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = { change(-1) }, modifier = Modifier.weight(1f).height(62.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = Panel2, contentColor = White)) { Text("− 1", fontSize = 22.sp) }
            Button(onClick = reset, modifier = Modifier.weight(1f).height(62.dp), colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Bg)) { Text("RESET", fontWeight = FontWeight.Bold) }
            FilledTonalButton(onClick = { change(1) }, modifier = Modifier.weight(1f).height(62.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = Panel2, contentColor = White)) { Text("+ 1", fontSize = 22.sp) }
        }
        Spacer(Modifier.height(18.dp))
        Slider(value = value.toFloat(), onValueChange = { change(it.toInt() - value) }, valueRange = -12f..12f, steps = 23, colors = SliderDefaults.colors(thumbColor = Gold, activeTrackColor = Gold, inactiveTrackColor = Panel2))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("−12", color = Muted); Text("0", color = Muted); Text("+12", color = Muted) }
        Spacer(Modifier.height(20.dp))
        Text("В тази първа версия стойността е локална. Изпращането на транспониране към M3 ще бъде включено след тест с реалния инструмент, за да избегнем двойно транспониране.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ColumnScope.ThirdsPage(enabled: Boolean, setEnabled: (Boolean) -> Unit, type: String, setType: (String) -> Unit) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("THIRDS STUDIO", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Хармонизиране и избор на интервал", color = Muted, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Терци", color = White, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("Включване на режима", color = Muted, fontSize = 12.sp) }
            Switch(checked = enabled, onCheckedChange = setEnabled, colors = SwitchDefaults.colors(checkedThumbColor = Bg, checkedTrackColor = Gold))
        }
        listOf("Голяма терца", "Малка терца", "Автоматично по скалата").forEach { item ->
            Row(Modifier.fillMaxWidth().background(if (type == item) Color(0xFF342A19) else Panel, RoundedCornerShape(12.dp)).clickable { setType(item) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(item, color = if (type == item) Gold else White, fontWeight = FontWeight.SemiBold); Text(when(item) { "Голяма терца" -> "+4 полутона"; "Малка терца" -> "+3 полутона"; else -> "Изчислява се от избраната скала" }, color = Muted, fontSize = 11.sp) }
                RadioButton(selected = type == item, onClick = { setType(item) }, colors = RadioButtonDefaults.colors(selectedColor = Gold))
            }
        }
        Text("Това е интерфейс за конфигуриране. Реалното хармонизиране изисква MIDI routing или поддръжка от самия инструмент; то не се изпраща към M3 в тази версия.", color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ColumnScope.PresetsPage(selected: String, choose: (String) -> Unit) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("MY SCALES", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Избери примерна скала за редактиране", color = Muted, fontSize = 12.sp)
        listOf("Равномерна", "Балканска", "Ориенталска").forEach { name ->
            Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).clickable { choose(name) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(if (name == selected) Gold else Panel2, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("♪", color = if (name == selected) Bg else Gold, fontSize = 22.sp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text(name, color = White, fontWeight = FontWeight.Bold); Text(if (name == "Равномерна") "Всички тонове по 0 цента" else "Демонстрационен шаблон за редакция", color = Muted, fontSize = 11.sp) }
                Text("Отвори →", color = Gold, fontSize = 12.sp)
            }
        }
        Text("Шаблоните Балканска и Ориенталска са демонстрационни и не претендират да са универсални традиционни настройки.", color = Muted, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
    }
}

private fun presetValues(name: String): List<Int> = when (name) {
    "Балканска" -> List(128) { i -> when (i % 12) { 1 -> -15; 3 -> 12; 6 -> -10; 8 -> 10; 10 -> -12; else -> 0 } }
    "Ориенталска" -> List(128) { i -> when (i % 12) { 1 -> -50; 3 -> 0; 4 -> -14; 6 -> 0; 8 -> -50; 10 -> -14; else -> 0 } }
    else -> List(128) { 0 }
}

private fun transposeLabel(value: Int): String = when (value) {
    0 -> "Без транспониране"
    else -> if (value > 0) "+$value полутона" else "$value полутона"
}
