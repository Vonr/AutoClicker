import org.jnativehook.GlobalScreen
import org.jnativehook.NativeHookException
import org.jnativehook.keyboard.NativeKeyEvent
import org.jnativehook.keyboard.NativeKeyListener
import org.jnativehook.mouse.NativeMouseEvent
import org.jnativehook.mouse.NativeMouseInputListener
import java.awt.BorderLayout
import java.awt.GridLayout
import java.awt.Robot
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.InputEvent
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.concurrent.ThreadLocalRandom
import javax.swing.*
import kotlin.math.abs
import kotlin.system.exitProcess
import java.lang.Math.max as max1


class AutoClicker : ActionListener, NativeMouseInputListener, NativeKeyListener, Robot() {

    private var totalProfiled = 0
    private var numProfiled = -1
    private var profiledDelay = mutableListOf<Int>()
    private var profiledDevn = mutableListOf<Int>()

    private var lastTime : Long = -1
    private var avgDelay = 0
    private var avgCps = 0.0
    private var avgDevn = 0
    private var mousePos : Pair<Int, Int> = Pair(-1, -1)
    var nextDelay = 1000
    var nextDevn = 0

    private var clickerIsOn = false
    private var startClicking = -1L
    private var shouldClick = -1L

    private var frame : JFrame = JFrame()
    private var panel : JPanel = JPanel()

    private var click = JButton()
    private var resetClicks = JButton()
    private var profiledLabel = JLabel()
    private var lastDelayLabel = JLabel()
    private var lastDevnLabel = JLabel()
    private var avgDelayLabel = JLabel()
    private var cpsLabel = JLabel()
    private var avgDevnLabel = JLabel()
    private var nextDelayLabel = JLabel()
    private var toggleClicker = JButton()
    private var clickingLabel = JLabel()
    private var autoCpsLabel = JLabel()

    private val df = DecimalFormat("0.00")

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            AutoClicker()
        }
    }

    init {
        try {
            GlobalScreen.registerNativeHook()
        } catch (ex: NativeHookException) {
            System.err.println("There was a problem registering the native hook.")
            System.err.println(ex.message)
            exitProcess(1)
        }

        GlobalScreen.addNativeMouseListener(this)
        GlobalScreen.addNativeMouseMotionListener(this)
        GlobalScreen.addNativeKeyListener(this)

        df.roundingMode = RoundingMode.HALF_UP

        click = JButton("Click Here")
        click.addActionListener(this)
        profiledLabel = JLabel("Clicks: 0")
        resetClicks = JButton("Reset Clicks")
        resetClicks.addActionListener(this)
        lastDelayLabel = JLabel("Last Delay: 0")
        lastDevnLabel = JLabel("Last Devn: 0")
        cpsLabel = JLabel("Avg. CPS: 0.00")
        avgDelayLabel = JLabel("Avg. Delay: 0")
        cpsLabel = JLabel("Avg. CPS: 0.00")
        avgDevnLabel = JLabel("Avg. Devn: 0")
        nextDelayLabel = JLabel("Next Delay: 0")
        toggleClicker = JButton("AutoClicker (F12): false")
        toggleClicker.addActionListener(this)
        clickingLabel = JLabel("Clicking: false")
        autoCpsLabel = JLabel("AutoClicker CPS: 0.00")

        panel.border = BorderFactory.createEmptyBorder(160, 160, 160, 160)
        panel.layout = GridLayout(0, 1)

        panel.add(click)
        panel.add(profiledLabel)
        panel.add(resetClicks)
        panel.add(lastDelayLabel)
        panel.add(lastDevnLabel)
        panel.add(avgDelayLabel)
        panel.add(cpsLabel)
        panel.add(avgDevnLabel)
        panel.add(nextDelayLabel)
        panel.add(toggleClicker)
        panel.add(clickingLabel)
        panel.add(autoCpsLabel)

        frame.add(panel, BorderLayout.CENTER)
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.title = "AutoClicker by Qther"
        frame.pack()
        frame.isVisible = true

        Thread {
            var randPercent = 0
            while (true) {
                var releaseTime = 5L
                if (totalProfiled > 2) {
                    profiledDelay = (profiledDelay.sortedDescending()).toMutableList()
                    profiledDevn = (profiledDevn.sortedDescending()).toMutableList()
                    val nextDelayPos = ThreadLocalRandom.current().nextInt(0, profiledDelay.size - 1)
                    val nextDevnPos = ThreadLocalRandom.current().nextInt(0, profiledDevn.size)
                    nextDevn = ThreadLocalRandom.current().nextInt(abs(profiledDevn[nextDevnPos]) + 1)
                    nextDelay = ThreadLocalRandom.current().nextInt(profiledDelay.last() - 2, profiledDelay[nextDelayPos]) + nextDevn
                    releaseTime = ThreadLocalRandom.current().nextLong(5, 15)
                    nextDelayLabel.text = "Next Delay: $nextDelay"
                    if (clickerIsOn && shouldClick >= 0) {
                        mousePress(InputEvent.BUTTON1_DOWN_MASK)
                        randPercent = ThreadLocalRandom.current().nextInt(30, 71)
                        Thread.sleep(releaseTime)
                        mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
                        Thread.sleep(5)
                        //nativeMousePressed(NativeMouseEvent(NativeMouseEvent.NATIVE_MOUSE_PRESSED, 0x00, mousePos.first, mousePos.second, 1, NativeMouseEvent.BUTTON1))
                        shouldClick = System.currentTimeMillis() - startClicking
                        clickingLabel.text = "Clicking: ${clickerIsOn && shouldClick >= 0}"
                    }
                }
                if (nextDelay > 0) autoCpsLabel.text = "AutoClicker CPS: ${df.format(1000.0 / nextDelay)}"
                //if (ThreadLocalRandom.current().nextBoolean()) Thread.sleep((nextDelay + nextDevn / 2 / 100 * (100 - randPercent)).toLong()) else Thread.sleep(abs((nextDelay - nextDevn / 2 / 100 * (100 - randPercent))).toLong())
                Thread.sleep(max1(5, nextDelay.toLong() - releaseTime - 5))
                randPercent = 0
            }
        }.start()
    }

    fun resetCurrent() {
        numProfiled = -1
        profiledLabel.text = "Clicks: $totalProfiled"
        lastTime = System.currentTimeMillis()
    }

    override fun actionPerformed(e: ActionEvent?) {
        if (e != null) {
            clickingLabel.text = "Clicking: ${clickerIsOn && shouldClick >- 0}"
            if (e.source is JButton) {
                if ((e.source as JButton).text.equals("Click Here", true)) {
                    if (System.currentTimeMillis() - lastTime > 300) {
                        resetCurrent()
                        return;
                    }
                    totalProfiled++
                    numProfiled++
                    if (numProfiled > -1) {
                        profiledDelay.add((System.currentTimeMillis() - lastTime).toInt())
                        if (profiledDelay.size >= 2) profiledDevn.add(abs(profiledDelay.last() - profiledDelay[profiledDelay.size - 2]))
                        var totalDelay = 0;
                        val delayIter = profiledDelay.iterator()
                        while (delayIter.hasNext()) {
                            totalDelay += delayIter.next()
                        }
                        var totalDevn = 0;
                        val devnIter = profiledDevn.iterator()
                        while (devnIter.hasNext()) {
                            totalDevn += devnIter.next()
                        }
                        avgDelay = totalDelay / profiledDelay.size
                        if (profiledDevn.size > 0) avgDevn = totalDevn / profiledDevn.size

                        lastDelayLabel.text = "Last Delay: ${System.currentTimeMillis() - lastTime}"
                        if (profiledDevn.size > 0) lastDevnLabel.text = "Last Devn: ${profiledDevn.last()}"
                    }
                    profiledDelay = (profiledDelay.sortedDescending()).toMutableList()
                    profiledDevn = (profiledDevn.sortedDescending()).toMutableList()
                    avgCps = df.format(1000.0 / avgDelay).toDouble()

                    profiledLabel.text = "Clicks: $totalProfiled"
                    avgDelayLabel.text = "Avg. Delay: $avgDelay"
                    cpsLabel.text = "Avg. CPS: $avgCps"
                    avgDevnLabel.text = "Avg. Devn: $avgDevn"

                    lastTime = System.currentTimeMillis();
                } else if ((e.source as JButton).text.startsWith("AutoClicker (F12): ", true)) {
                    clickerIsOn = !clickerIsOn
                    startClicking = System.currentTimeMillis()
                    if (!clickerIsOn) shouldClick = 0
                    toggleClicker.text = "AutoClicker (F12): $clickerIsOn"
                } else if ((e.source as JButton).text.equals("Reset Clicks", true)) {
                    totalProfiled = 0
                    avgDelay = 0
                    avgCps = 0.0
                    avgDevn = 0
                    profiledDelay.clear()
                    profiledDevn.clear()
                    nextDelay = 1000
                    nextDevn = 0
                    profiledLabel.text = "Clicks: 0"
                    avgDelayLabel.text = "Avg. Delay: 0"
                    cpsLabel.text = "Avg. CPS: 0.00"
                    avgDevnLabel.text = "Avg. Devn: 0"
                    nextDelayLabel.text = "Next Delay: 0"
                    lastDelayLabel.text = "Last Delay: 0"
                    lastDevnLabel.text = "Last Devn: 0"
                    autoCpsLabel.text = "AutoClicker CPS: 1.00"
                }
            }
        }
    }

    override fun nativeMousePressed(p0: NativeMouseEvent?) {
        if (p0 != null && clickerIsOn) {
            if (p0.button == NativeMouseEvent.BUTTON1) {
                startClicking = System.currentTimeMillis()
                shouldClick = 0
                clickingLabel.text = "Clicking: ${clickerIsOn && shouldClick >= 0}"
            }
        }
    }

    override fun nativeMouseMoved(nativeEvent: NativeMouseEvent?) {
        if (nativeEvent != null) {
            mousePos = Pair(nativeEvent.x, nativeEvent.y)
        }

    }

    override fun nativeMouseClicked(p0: NativeMouseEvent?) {
    }

    override fun nativeMouseReleased(p0: NativeMouseEvent?) {
        if (p0 != null && clickerIsOn) {
            if (p0.button == NativeMouseEvent.BUTTON1) {
                startClicking = System.currentTimeMillis()
                shouldClick = 0
                clickingLabel.text = "Clicking: ${clickerIsOn && shouldClick >= 0}"
            }
        }
    }

    override fun nativeMouseDragged(nativeEvent: NativeMouseEvent?) {
    }

    override fun nativeKeyTyped(nativeEvent: NativeKeyEvent?) {
    }

    override fun nativeKeyPressed(nativeEvent: NativeKeyEvent?) {
        if (nativeEvent != null) {
            if (nativeEvent.keyCode == NativeKeyEvent.VC_F12) {
                clickerIsOn = !clickerIsOn
                clickingLabel.text = "Clicking: ${clickerIsOn && shouldClick >= 0}"
                toggleClicker.text = "AutoClicker (F12): $clickerIsOn"
            }
        }
    }

    override fun nativeKeyReleased(nativeEvent: NativeKeyEvent?) {
    }
}