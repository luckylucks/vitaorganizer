package com.soywiz.vitaorganizer.popups

import com.soywiz.vitaorganizer.*
import com.soywiz.vitaorganizer.ext.action
import java.awt.event.KeyEvent
import java.awt.Container
import java.awt.Component
import java.awt.event.KeyAdapter
import java.io.File
import java.util.*
import javax.swing.*

class RenamerFrame(val vita: VitaOrganizer, val entry: GameEntry, title: String) : JFrame(title) {

    val renamer = JTextField(VitaOrganizerSettings.renamerString).action { updateRenamedString() }
    val output = JTextField("Test")

    init {
        setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE )
        val panel = JPanel(SpringLayout()).apply {
            add(JLabel("Original Filename:", JLabel.TRAILING))
            add(JLabel(entry.vpkLocalFile?.name))
            add(JLabel("Parameter:", JLabel.TRAILING))

            val builder = StringJoiner(", ")
            for (r in RenamerStrings.values()) {
                builder.add( r.short + " - " + r.description )
            }
            add(JLabel( builder.toString() ))
            add(JLabel("Renamerstring:", JLabel.TRAILING))
            add(renamer)
            add(JLabel("New Name:", JLabel.TRAILING))
            add(output)
            add(JButton("Cancel").action { this@RenamerFrame.dispose()} )
            add(JButton("Rename").action { renameFile() } )

        }
        panel.setSize(640, 480)
        SpringUtilities.makeCompactGrid(panel, 5, 2,
                6, 6, 6, 6)

        setContentPane(panel)
        pack()
        val keyListener = object : KeyAdapter() {
            override fun keyReleased(keyEvent: KeyEvent) {
                updateRenamedString()
            }
        }
        renamer.addKeyListener(keyListener)
        updateRenamedString()
    }

    fun updateRenamedString() {
        var text = renamer.text
        for( r in RenamerStrings.values() ) {
            text = text.replace( r.short, r.value(entry) )
        }
        output.setText(text)
        println("Formated Text ${text}")
    }

    fun renameFile() {
        val file = entry.vpkLocalFile
        val path = file?.path?.replace(file.name.toString(), "")
        val newname = path + output.text
        println("Rename from ${file?.name} to ${newname}" )
        file?.renameTo( File(newname) )
        VitaOrganizerSettings.renamerString = renamer.text
        vita.updateFileList()
        this.dispose()
    }
}

enum class RenamerStrings(val short: String, val description: String, val value: (entry: GameEntry)->String) { //, val value: Any
    TITLE("%TITLE%", "Title", {entry: GameEntry -> entry.title}),
    ID("%ID%", "ID", {entry: GameEntry -> entry.gameId}),
    LANGUAGE("%REG%", "Region", {entry: GameEntry -> entry.region().short}),
    DUMPER("%VT%", "Dumper", {entry: GameEntry -> entry.dumperVersionShort}),
    COMPRESSION("%COMP%", "Compression", {entry: GameEntry -> entry.compressionLevel}),
    VERSION("%VER%", "Version", {entry: GameEntry -> (entry.psf["APP_VER"] ?: entry.psf["VERSION"] ?: Texts.format("UNKNOWN_VERSION")).toString() }),
}

internal object SpringUtilities {
    /**
     * A debugging utility that prints to stdout the component's minimum,
     * preferred, and maximum sizes.
     */
    fun printSizes(c: Component) {
        System.out.println("minimumSize = " + c.getMinimumSize())
        System.out.println("preferredSize = " + c.getPreferredSize())
        System.out.println("maximumSize = " + c.getMaximumSize())
    }

    /**
     * Aligns the first `rows` * `cols` components of
     * `parent` in a grid. Each component is as big as the maximum
     * preferred width and height of the components. The parent is made just big
     * enough to fit them all.

     * @param rows
     * *          number of rows
     * *
     * @param cols
     * *          number of columns
     * *
     * @param initialX
     * *          x location to start the grid at
     * *
     * @param initialY
     * *          y location to start the grid at
     * *
     * @param xPad
     * *          x padding between cells
     * *
     * @param yPad
     * *          y padding between cells
     */
    fun makeGrid(parent: Container, rows: Int, cols: Int, initialX: Int, initialY: Int,
                 xPad: Int, yPad: Int) {
        val layout: SpringLayout
        try {
            layout = parent.getLayout() as SpringLayout
        } catch (exc: ClassCastException) {
            System.err.println("The first argument to makeGrid must use SpringLayout.")
            return
        }

        val xPadSpring = Spring.constant(xPad)
        val yPadSpring = Spring.constant(yPad)
        val initialXSpring = Spring.constant(initialX)
        val initialYSpring = Spring.constant(initialY)
        val max = rows * cols

        // Calculate Springs that are the max of the width/height so that all
        // cells have the same size.
        var maxWidthSpring = layout.getConstraints(parent.getComponent(0)).width
        var maxHeightSpring = layout.getConstraints(parent.getComponent(0)).width
        for (i in 1..max - 1) {
            val cons = layout.getConstraints(parent.getComponent(i))

            maxWidthSpring = Spring.max(maxWidthSpring, cons.width)
            maxHeightSpring = Spring.max(maxHeightSpring, cons.height)
        }

        // Apply the new width/height Spring. This forces all the
        // components to have the same size.
        for (i in 0..max - 1) {
            val cons = layout.getConstraints(parent.getComponent(i))

            cons.width = maxWidthSpring
            cons.height = maxHeightSpring
        }

        // Then adjust the x/y constraints of all the cells so that they
        // are aligned in a grid.
        var lastCons: SpringLayout.Constraints? = null
        var lastRowCons: SpringLayout.Constraints? = null
        for (i in 0..max - 1) {
            val cons = layout.getConstraints(parent.getComponent(i))
            if (i % cols == 0) { // start of new row
                lastRowCons = lastCons
                cons.x = initialXSpring
            } else { // x position depends on previous component
                cons.x = Spring.sum(lastCons!!.getConstraint(SpringLayout.EAST), xPadSpring)
            }

            if (i / cols == 0) { // first row
                cons.y = initialYSpring
            } else { // y position depends on previous row
                cons.y = Spring.sum(lastRowCons!!.getConstraint(SpringLayout.SOUTH), yPadSpring)
            }
            lastCons = cons
        }

        // Set the parent's size.
        val pCons = layout.getConstraints(parent)
        pCons.setConstraint(SpringLayout.SOUTH, Spring.sum(Spring.constant(yPad), lastCons!!.getConstraint(SpringLayout.SOUTH)))
        pCons.setConstraint(SpringLayout.EAST, Spring.sum(Spring.constant(xPad), lastCons.getConstraint(SpringLayout.EAST)))
    }

    /* Used by makeCompactGrid. */
    private fun getConstraintsForCell(row: Int, col: Int, parent: Container,
                                      cols: Int): SpringLayout.Constraints {
        val c = parent.getComponent(row * cols + col)
        val layout: SpringLayout = parent.layout as SpringLayout
        return layout.getConstraints(c)
    }

    /**
     * Aligns the first `rows` * `cols` components of
     * `parent` in a grid. Each component in a column is as wide as
     * the maximum preferred width of the components in that column; height is
     * similarly determined for each row. The parent is made just big enough to
     * fit them all.

     * @param rows
     * *          number of rows
     * *
     * @param cols
     * *          number of columns
     * *
     * @param initialX
     * *          x location to start the grid at
     * *
     * @param initialY
     * *          y location to start the grid at
     * *
     * @param xPad
     * *          x padding between cells
     * *
     * @param yPad
     * *          y padding between cells
     */
    fun makeCompactGrid(parent: Container, rows: Int, cols: Int, initialX: Int,
                        initialY: Int, xPad: Int, yPad: Int) {
        val layout: SpringLayout
        try {
            layout = parent.getLayout() as SpringLayout
        } catch (exc: ClassCastException) {
            System.err.println("The first argument to makeCompactGrid must use SpringLayout.")
            return
        }

        // Align all cells in each column and make them the same width.
        var x = Spring.constant(initialX)
        for (c in 0..cols - 1) {
            var width = Spring.constant(0)
            for (r in 0..rows - 1) {
                width = Spring.max(width, getConstraintsForCell(r, c, parent, cols).width)
            }
            for (r in 0..rows - 1) {
                val constraints = getConstraintsForCell(r, c, parent, cols)
                constraints.x = x
                constraints.width = width
            }
            x = Spring.sum(x, Spring.sum(width, Spring.constant(xPad)))
        }

        // Align all cells in each row and make them the same height.
        var y = Spring.constant(initialY)
        for (r in 0..rows - 1) {
            var height = Spring.constant(0)
            for (c in 0..cols - 1) {
                height = Spring.max(height, getConstraintsForCell(r, c, parent, cols).height)
            }
            for (c in 0..cols - 1) {
                val constraints = getConstraintsForCell(r, c, parent, cols)
                constraints.y = y
                constraints.height = height
            }
            y = Spring.sum(y, Spring.sum(height, Spring.constant(yPad)))
        }

        // Set the parent's size.
        val pCons = layout.getConstraints(parent)
        pCons.setConstraint(SpringLayout.SOUTH, y)
        pCons.setConstraint(SpringLayout.EAST, x)
    }
}
