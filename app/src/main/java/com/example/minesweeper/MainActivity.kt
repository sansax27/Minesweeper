package com.example.minesweeper

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach

class MainActivity : AppCompatActivity() {

    // Shared Preferences To Get And Store Data
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Getting the Radio Group Of Choosing Difficulty Level
        val chooseDifficultyRadioGroup = findViewById<RadioGroup>(R.id.difficultyRadioButtons)

        sharedPreferences = getSharedPreferences("Default",Context.MODE_PRIVATE)

        //Getting Screen Width in DP and dividing by 10 so that Number Of Columns less than Width/10 are not accepted to give better UI Experience
        val numberOfColumnsAllowed = resources.configuration.screenWidthDp / 10


        //Getting Inputs Views
        val numberOfRowsInput = findViewById<EditText>(R.id.numberOfRows)
        val numberOfColumnsInput = findViewById<EditText>(R.id.numberOfColumns)
        val numberOfMinesInput = findViewById<EditText>(R.id.numberOfMines)

        // Getting Custom Board Making Button
        val makeACustomBoard = findViewById<TextView>(R.id.makeACustomBoard)

        //Text and Toast to show when User gives Invalid Input
        val invalidInput =
            "You must satisfy all these Conditions to be able to start the Game.\n1.${getString(R.string.mainActivityNumberOfRowsNotEmpty)}" +
                    "\n2.${getString(R.string.mainActivityNumberOfColumnsNotEmpty)}" +
                    "\n3.${getString(R.string.mainActivityNumberOfMinesNotEmpty)}" +
                    "\n4.${getString(R.string.mainActivityNumberOfMinesNotGreaterThanOneForthOfRowsMultipliedColumns)}"+
                    "\n5.${getString(R.string.mainActivityNumberOfColumnsNotGoodUI)}"
        val invalidInputAlert = AlertDialog.Builder(this).setMessage(invalidInput)

        // Enabling and Disabling UI for Custom Board based on the current text of Make A Custom Board Button
        makeACustomBoard.setOnClickListener {
            val button = (it as? Button)!!
            if (button.text.equals(getString(R.string.mainActivityMakeACustomBoard))) {
                //When user clicks on Make A CustomBoard Button, Difficulty Options are Disabled and Custom Make Board Options Are Disabled
                button.text = getString(R.string.mainActivityMakeACustomBoardRevert)
                chooseDifficultyRadioGroup.forEach { view ->
                    view.isEnabled = false
                }
                numberOfRowsInput.isEnabled = true
                numberOfColumnsInput.isEnabled = true
                numberOfMinesInput.isEnabled = true
                Toast.makeText(
                    this,
                    getString(R.string.mainActivityMakeACustomBoardToast),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                //When Else is the case, the changes above are reversed
                button.text = getString(R.string.mainActivityMakeACustomBoard)
                chooseDifficultyRadioGroup.forEach { view ->
                    view.isEnabled = true
                }
                numberOfRowsInput.isEnabled = false
                numberOfColumnsInput.isEnabled = false
                numberOfMinesInput.isEnabled = false
                Toast.makeText(
                    this,
                    getString(R.string.mainActivityMakeACustomBoardToastRevert),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val startButton = findViewById<Button>(R.id.startGame)
        startButton.setOnClickListener {
            if (makeACustomBoard.text == getString(R.string.mainActivityMakeACustomBoard)) {

                // If No Difficulty Level Selected, Then Showing Toast
                if (chooseDifficultyRadioGroup.checkedRadioButtonId == -1) {
                    Toast.makeText(this, getString(R.string.chooseDifficulty), Toast.LENGTH_LONG)
                        .show()
                } else {

                    // Checking Selected Difficulty Level And Only If Dimensions Of Device Is Eligible For A Good UI,
                    // Then Only Going To Play Activity
                    // Also Setting Rows And Columns For Particular Difficulty Level
                    val difficultyWiseDimension =
                        when (chooseDifficultyRadioGroup.checkedRadioButtonId) {
                            R.id.easy -> {
                                if (3 > numberOfColumnsAllowed) {
                                    Toast.makeText(
                                        this,
                                        getString(R.string.cantSelectLevel).format("Easy"),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    -1
                                } else {
                                    3
                                }
                            }
                            R.id.medium -> {
                                if (5 > numberOfColumnsAllowed) {
                                    Toast.makeText(
                                        this,
                                        getString(R.string.cantSelectLevel).format("Medium"),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    -1
                                } else {
                                    5
                                }
                            }
                            R.id.difficult -> {
                                if (7 > numberOfColumnsAllowed) {
                                    Toast.makeText(
                                        this,
                                        getString(R.string.cantSelectLevel).format("Difficult"),
                                        Toast.LENGTH_LONG
                                    ).show()
                                    -1
                                } else {
                                    7
                                }
                            }
                            else -> -1
                        }
                    if (difficultyWiseDimension != -1) {
                        // Transferring to play Game Activity
                        val playGameIntent = Intent(this, PlayMinesweeperGame::class.java).apply {
                            putExtra("numberOfRows", difficultyWiseDimension)
                            putExtra("numberOfColumns", difficultyWiseDimension)
                            putExtra("numberOfMines", when(difficultyWiseDimension) {
                                3 -> 1
                                5 -> 4
                                7 -> 12
                                else -> 36
                            })
                        }
                        startActivity(playGameIntent)
                    }

                }
            } else {
                if (numberOfRowsInput.text.isEmpty() || numberOfColumnsInput.text.isEmpty() || numberOfMinesInput.text.isEmpty() || (numberOfColumnsInput.text.toString()).toInt() >= numberOfColumnsAllowed) {
                    invalidInputAlert.show()
                } else {
                    val numberOfMinesAllowed =
                        ((numberOfRowsInput.text.toString()).toInt()) * ((numberOfColumnsInput.text.toString()).toInt()) / 4
                    if ((numberOfMinesInput.text.toString()).toInt() > numberOfMinesAllowed) {
                        invalidInputAlert.show()
                    } else {
                        // Transferring to play Game Activity
                        val playGameIntent = Intent(this, PlayMinesweeperGame::class.java).apply {
                            putExtra("numberOfRows", numberOfRowsInput.text.toString().toInt())
                            putExtra(
                                "numberOfColumns",
                                numberOfColumnsInput.text.toString().toInt()
                            )
                            putExtra("numberOfMines", numberOfMinesInput.text.toString().toInt())
                        }
                        startActivity(playGameIntent)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val bestGameTime = findViewById<TextView>(R.id.bestGameTime)
        val lastGameTime = findViewById<TextView>(R.id.lastGameTime)

        //If Previously Game Was Played, then displays the value, otherwise other text
        if (sharedPreferences.getInt("bestTime", -1) == -1) {
            bestGameTime.text = getString(R.string.mainActivityBestTimeNotAvailable)
            lastGameTime.text = getString(R.string.mainActivityLastGameTimeNotAvailable)
        } else {
            sharedPreferences.getInt("bestTime", Int.MAX_VALUE).apply {
                if (this>60) {
                    bestGameTime.text = getString(R.string.mainActivityBestTime).format((this/60).toString()+"m ", (this-((this/60)*60)).toString()+"s")
                } else {
                    bestGameTime.text = getString(R.string.mainActivityBestTime).format("", this.toString()+"s")
                }
            }

            sharedPreferences.getInt("lastTime", Int.MAX_VALUE).apply {
                if (this>60) {
                    lastGameTime.text = getString(R.string.mainActivityLastGameTime).format((this/60).toString()+"m ", (this-((this/60)*60)).toString()+"s")
                } else {
                    lastGameTime.text = getString(R.string.mainActivityLastGameTime).format("", this.toString()+"s")
                }
            }
        }
    }

}