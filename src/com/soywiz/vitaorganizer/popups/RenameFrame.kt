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

class RenamerFrame(val vita: VitaOrganizer, val entry: GameEntry) : JFrame() {

    val renamer = JTextField(VitaOrganizerSettings.renamerString).action { updateRenamedString() }
    val output = JTextField("Test")

    init {
        setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE )
        setTitle(Texts.format("RENAMER_FRAMETITLE", "game" to entry.title))
        val panel = JPanel(SpringLayout()).apply {
            add(JLabel(Texts.format("RENAMER_ORIGINALNAME"), JLabel.TRAILING))
            add(JLabel(entry.vpkLocalFile?.name))
            add(JLabel(Texts.format("RENAMER_PARAMETER"), JLabel.TRAILING))

            val builder = StringJoiner(", ")
            for (r in RenamerStrings.values()) {
                builder.add( r.short + " - " + r.description )
            }
            add(JLabel( builder.toString() ))
            add(JLabel(Texts.format("RENAMER_RENAMERSTRING"), JLabel.TRAILING))
            add(renamer)
            add(JLabel(Texts.format("RENAMER_NEWNAME"), JLabel.TRAILING))
            add(output)
            add(JButton(Texts.format("RENAMER_BUTTON_CANCEL")).action { this@RenamerFrame.dispose()} )
            add(JButton(Texts.format("RENAMER_BUTTON_RENAME")).action { renameFile() } )

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
            text = text.replace(r.short, r.value(entry))
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
        VitaOrganizerCache.entry( entry.gameId ).delete()
        vita.updateFileList()
        this.dispose()
    }
}

enum class RenamerStrings(val short: String, val description: String, val value: (entry: GameEntry)->String) {
    TITLE("%TITLE%", Texts.format("RENAMERSTRINGS_TITLE"), {entry: GameEntry -> entry.title}),
    ID("%ID%", Texts.format("RENAMERSTRINGS_ID"), {entry: GameEntry -> entry.gameId}),
    LANGUAGE("%REG%", Texts.format("RENAMERSTRINGS_REGION"), {entry: GameEntry -> entry.region().short}),
    DUMPER("%VT%", Texts.format("RENAMERSTRINGS_DUMPER"), {entry: GameEntry -> entry.dumperVersionShort}),
    COMPRESSION("%COMP%", Texts.format("RENAMERSTRINGS_COMPRESSION"), {entry: GameEntry -> entry.compressionLevel}),
    VERSION("%VER%", Texts.format("RENAMERSTRINGS_VERSION"), {entry: GameEntry -> (entry.psf["APP_VER"] ?: entry.psf["VERSION"] ?: Texts.format("UNKNOWN_VERSION")).toString() }),
}

/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/**
 * A 1.4 file that provides utility methods for
 * creating form- or grid-style layouts with SpringLayout.
 * These utilities are used by several programs, such as
 * SpringBox and SpringCompactGrid.
 *
 * automatic transformed to kotlin
 */
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
