package com.example.minesweeper

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.gridlayout.widget.GridLayout
import java.util.*
import kotlin.random.Random


class PlayMinesweeperGame : AppCompatActivity() {

    // List To Store Location Of Mines
    private val mineSites = mutableListOf<MutableList<Int>>()

    // Variable To Know That A Click Is First Clicks
    private var firstTime = true

    // Storing Reference To All Buttons
    private val allButtons = mutableListOf<MutableList<Button>>()

    // Handler to Implement Timer
    private val handler = Handler(Looper.getMainLooper())


    // Start Time To Make Timer Function
    private var startTime = 0L


    // Number Of Minus Flags
    private var numberOfMinesMinusFlagsValue = 0

    // Store The Won Status Of Game
    private var continueGame = true

    // Storing Time Passed
    private var timePassed = 0

    // Store The Number Of Sites Explored
    private var sitesExplored = 0

    // Storing Statuses Of All Buttons
    private val buttonStatuses = mutableListOf<MutableList<ButtonStatus>>()

    // ButtonStatus -> Enum Class To Store Button Statuses

    // Shared Preferences To Get And Store Data
    private lateinit var sharedPreferences: SharedPreferences


    inner class StopWatchObject : Runnable {
        override fun run() {
            if (continueGame) {
                timePassed = ((Calendar.getInstance().timeInMillis - startTime) / 1000).toInt()
                if (timePassed > 60) {
                    findViewById<TextView>(R.id.timePassed).text =
                        getString(R.string.displayTimeSeconds).format(
                            (timePassed / 60).toString() + "m ",
                            (timePassed - ((timePassed / 60) * 60)).toString() + "s"
                        )
                } else {
                    findViewById<TextView>(R.id.timePassed).text =
                        getString(R.string.displayTimeSeconds).format(
                            "",
                            timePassed.toString() + "s"
                        )
                }
                handler.postDelayed(this, 1000)
            }
        }
    }

    // StopWatch Object To Implement StopWatch
    private val stopWatchObject = StopWatchObject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_minesweeper_game)

        val numberOfMinesMinusFlags = findViewById<TextView>(R.id.numberOfMinesMinusFlags)

        // The Number Of Mines
        val numberOfMines = intent.getIntExtra("numberOfMines", 0)


        // Setting Bezel Values
        numberOfMinesMinusFlags.text = numberOfMines.toString()

        // Setting Initial Value
        numberOfMinesMinusFlagsValue = numberOfMines

        //Getting Table to implement View Of Minesweeper
        val minesweeperPlayBoard = findViewById<GridLayout>(R.id.minesweeperPlayBoard)

        sharedPreferences = getSharedPreferences("Default",Context.MODE_PRIVATE)

        AlertDialog.Builder(this)
            .setMessage(getString(R.string.longClickToFlag) + "\n" + getString(R.string.clickToExplore))
            .show().setOnDismissListener {
                startTime = Calendar.getInstance().timeInMillis
                // Starting StopWatch
                handler.postDelayed(stopWatchObject, 0)
            }


        //Getting Number of Rows
        val numberOfRows: Int = intent.getIntExtra("numberOfRows", 2)




        minesweeperPlayBoard.rowCount = numberOfRows
        //Getting number of Columns
        val numberOfColumns: Int = intent.getIntExtra("numberOfColumns", 2)


        minesweeperPlayBoard.columnCount = numberOfColumns


        // Restarting Game i.e. Activity On Clicking Restart
        findViewById<Button>(R.id.restart).setOnClickListener {
            val playGameIntent = Intent(this, PlayMinesweeperGame::class.java).apply {
                putExtra("numberOfRows", numberOfRows)
                putExtra("numberOfColumns", numberOfColumns)
                putExtra("numberOfMines", numberOfMines)
            }
            startActivity(playGameIntent)
            this.finish()
        }

        // Function to Check If Index Is Valid While Checking For Mines Around A Site
        val isValidIndex = { a: Int, b: Int ->
            a>=0 && b>=0 && a<numberOfRows && b<numberOfColumns
        }

        for (i in 0 until numberOfRows) {

            //Setting Mutable List for this Row
            val rowButtonStatusList = mutableListOf<ButtonStatus>()
            val rowMinesList = mutableListOf<Int>()
            val rowButtonsList = mutableListOf<Button>()

            for (j in 0 until numberOfColumns) {
                // Setting Buttons for Game
                val button = Button(this)

                //Setting Initial Status as Hidden
                rowButtonStatusList.add(ButtonStatus.Hidden)

                //Setting Visuals for Buttons Initially
                button.setBackgroundColor(getColor(R.color.playGameMinesweeperButton))

                rowMinesList.add(0)

                // Setting Layout Params for Button
                val layoutForButtons = LinearLayout.LayoutParams(
                    TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        ((resources.configuration.screenWidthDp-5*(numberOfColumns+1)).toFloat() / numberOfColumns),
                        resources.displayMetrics
                    ).toInt(), TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        50f,
                        resources.displayMetrics
                    ).toInt()
                )
                layoutForButtons.setMargins(5, 5, 5, 5)
                button.layoutParams = layoutForButtons


                button.setOnClickListener {
                    // Setting Mines Only When First Click Is Done To Ensure That First Site Does Not Have Mine
                    if (firstTime) {
                        repeat(numberOfMines) {
                            var randomRow = Random.nextInt(numberOfRows)
                            var randomColumn = Random.nextInt(numberOfColumns)
                            while (mineSites[randomRow][randomColumn] == -1 || (randomRow == i && randomColumn == j)) {
                                randomColumn = Random.nextInt(numberOfColumns)
                                randomRow = Random.nextInt(numberOfRows)
                            }
                            mineSites[randomRow][randomColumn] = -1
                        }
                        firstTime = false
                    }

                    //Getting Status of Button And Doing Action According To It
                    when (rowButtonStatusList[j]) {
                        ButtonStatus.Hidden -> {
                            if (mineSites[i][j] != -1) {
                                button.setBackgroundColor(getColor(R.color.white))
                                rowButtonStatusList[j] = ButtonStatus.Explored
                                sitesExplored += 1
                                calculateMines(i, j, isValidIndex).apply {
                                    if (this!=0) {
                                        button.text = this.toString()
                                    } else {
                                        unTurnButtons(i, j, isValidIndex)
                                    }
                                    checkIfGameWonAndDo(
                                        numberOfMines,
                                        numberOfRows,
                                        numberOfColumns,
                                        isValidIndex
                                    )
                                }
                            } else {
                                exploreAllSites(isValidIndex)
                                continueGame = false
                                Toast.makeText(this, "Sorry!! You Lost!!", Toast.LENGTH_LONG).show()
                            }
                        }
                        ButtonStatus.Flagged -> {
                            button.setBackgroundColor(getColor(R.color.playGameMinesweeperButton))
                            button.foreground = null
                            rowButtonStatusList[j] = ButtonStatus.Hidden
                            if (mineSites[i][j] == -1) {
                                numberOfMinesMinusFlagsValue += 1
                                numberOfMinesMinusFlags.text =
                                    numberOfMinesMinusFlagsValue.toString()
                            } else {
                                sitesExplored -= 1
                            }
                        }
                        else -> {
                        }
                    }
                }

                button.setOnLongClickListener {
                    when (rowButtonStatusList[j]) {
                        ButtonStatus.Hidden -> {
                            button.setBackgroundColor(getColor(R.color.white))
                            button.foreground =
                                ContextCompat.getDrawable(this, R.drawable.ic_flag_vector)
                            rowButtonStatusList[j] = ButtonStatus.Flagged
                            if (mineSites[i][j] == -1) {
                                numberOfMinesMinusFlagsValue -= 1
                                numberOfMinesMinusFlags.text =
                                    numberOfMinesMinusFlagsValue.toString()
                                checkIfGameWonAndDo(
                                    numberOfMines,
                                    numberOfRows,
                                    numberOfColumns,
                                    isValidIndex
                                )
                            } else {
                                sitesExplored += 1
                            }
                        }
                        else -> {
                        }
                    }
                    true
                }
                minesweeperPlayBoard.addView(button)
                rowButtonsList.add(button)
            }
            mineSites.add(rowMinesList)
            allButtons.add(rowButtonsList)
            buttonStatuses.add(rowButtonStatusList)
        }
    }


    // Function To Calculate Number Of Mines Around A Site
    private fun calculateMines(row: Int, column: Int, isValidIndex: (Int, Int) -> Boolean): Int {
        var numberOfMines = 0
        if (isValidIndex(row, column - 1) && mineSites[row][column - 1]==-1) {
            numberOfMines++
        }
        if (isValidIndex(row, column + 1) && mineSites[row][column + 1]==-1) {
            numberOfMines++
        }
        if (isValidIndex(row - 1, column - 1) && mineSites[row - 1][column - 1] == -1) {
            numberOfMines++
        }
        if (isValidIndex(row - 1, column + 1) && mineSites[row - 1][column + 1]==-1) {
            numberOfMines++
        }
        if (isValidIndex(row - 1, column) && mineSites[row - 1][column]==-1) {
            numberOfMines++
        }
        if (isValidIndex(row + 1, column - 1) && mineSites[row + 1][column - 1]==-1) {
            numberOfMines++
        }
        if (isValidIndex(row + 1, column + 1) && mineSites[row + 1][column + 1]==-1) {
            numberOfMines++
        }
        if (isValidIndex(row + 1, column) && mineSites[row + 1][column]==-1) {
            numberOfMines++
        }
        return numberOfMines
    }


    // Function To UnTurn All Neighboring Sites When Zero Mines Are There Around the Site
    private fun unTurnButtons(row: Int, column: Int, isValidIndex: (Int, Int) -> Boolean) {
        if (isValidIndex(row, column - 1)) {
            allButtons[row][column - 1].callOnClick()
        }
        if (isValidIndex(row, column + 1)) {
            allButtons[row][column + 1]
        }
        if (isValidIndex(row - 1, column - 1)) {
            allButtons[row - 1][column - 1].callOnClick()
        }
        if (isValidIndex(row - 1, column + 1)) {
            allButtons[row - 1][column + 1].callOnClick()
        }
        if (isValidIndex(row - 1, column)) {
            allButtons[row - 1][column].callOnClick()
        }
        if (isValidIndex(row + 1, column - 1)) {
            allButtons[row + 1][column - 1].callOnClick()
        }
        if (isValidIndex(row + 1, column + 1)) {
            allButtons[row + 1][column + 1].callOnClick()
        }
        if (isValidIndex(row + 1, column)) {
            allButtons[row + 1][column].callOnClick()
        }
    }

    // Explore All Sites When Game Is Over
    private fun exploreAllSites(isValidIndex: (Int, Int) -> Boolean) {
        for (i in allButtons.indices) {
            for (j in allButtons[i].indices) {
                allButtons[i][j].setBackgroundColor(getColor(R.color.white))
                if (mineSites[i][j] == -1) {
                    allButtons[i][j].foreground =
                        ContextCompat.getDrawable(this, R.drawable.ic_mines)
                } else {
                    calculateMines(i, j, isValidIndex).apply {
                        if (this != 0) {
                            allButtons[i][j].text = this.toString()
                        }
                    }
                }
            }
        }
    }


    // Check If Game Won And Do The Required
    private fun checkIfGameWonAndDo(
        numberOfMines: Int,
        rows: Int,
        column: Int,
        isValidIndex: (Int, Int) -> Boolean
    ) {
        if (sitesExplored == (rows * column - numberOfMines) || numberOfMinesMinusFlagsValue == 0) {
            exploreAllSites(isValidIndex)
            continueGame = false
            Toast.makeText(this, "Congrats!! You Won!!", Toast.LENGTH_LONG).show()
            findViewById<GridLayout>(R.id.minesweeperPlayBoard).isEnabled = false
            sharedPreferences.edit().putInt("lastTime", timePassed).apply()
            if (sharedPreferences.getInt("bestTime", Int.MAX_VALUE) > timePassed) {
                sharedPreferences.edit().putInt("bestTime", timePassed).apply()
            }
            Log.i("lastTime", sharedPreferences.getInt("lastTime",-1).toString())
            Log.i("bestTime", sharedPreferences.getInt("bestTime",-1).toString())

        }
    }


}