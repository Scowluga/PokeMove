package com.scowluga.android.pokemon

import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.widget.*
import com.dd.processbutton.iml.ActionProcessButton
import com.scowluga.android.pokemon.model.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    val pokeApiService by lazy { // Singleton design pattern
        PokeApiService.create()
    }

    // effectiveness of the TypeRelations against a specific type
    fun checkEffectiveness(dr: TypeRelations, type: PokemonType) = when {
        dr.noDamageTo.any {it same type} -> 0.0
        dr.halfDamageTo.any {it same type} -> 0.5
        dr.doubleDamageTo.any {it same type} -> 2.0
        else -> 1.0
    }

    // epsilon check with an infix
    infix fun Double.near(d: Double): Boolean = (this - d) < 0.001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initializing Type AutoComplete
        val typeTV: TextInputAutoCompleteTextView = findViewById(R.id.atv1)
        val typeList = arrayOf(
                "Normal", "Fire", "Fighting", "Water", "Flying", "Grass", "Poison",
                "Electric", "Ground", "Psychic", "Rock", "Ice", "Bug", "Dragon",
                "Ghost", "Dark", "Steel", "Fairy"
        )

        val typeAdapter = ArrayAdapter<String> (
                this,
                android.R.layout.simple_dropdown_item_1line,
                typeList
        )

        typeTV.setAdapter(typeAdapter)
        typeTV.threshold = 1


        // Initializing Pokemon AutoComplete
        val pokeTV: TextInputAutoCompleteTextView = findViewById(R.id.atv2)

        val c0 = pokeApiService.getAllPokemon()
        c0.enqueue(object: Callback<NamedApiResourceList> {
            override fun onFailure(call: Call<NamedApiResourceList>?, t: Throwable?) {
                Toast.makeText(this@MainActivity, "Failed to load Pokemon", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<NamedApiResourceList>?, response: Response<NamedApiResourceList>?) {
                val pokeList = response?.body()?.results
                if (pokeList == null) {
                    Toast.makeText(this@MainActivity, "Failed to load pokemon", Toast.LENGTH_SHORT).show()
                    return
                }

                val pokeAdapter = ArrayAdapter<String> (
                        this@MainActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        pokeList.map { it.name.capitalize() } // wow Kotlin is powerful
                )

                pokeTV.setAdapter(pokeAdapter)
                pokeTV.threshold = 1
            }
        })

        // Display text
        val tv: TextView = findViewById(R.id.tv)

        // Button
        val btn: ActionProcessButton = findViewById(R.id.btn)
        btn.setMode(ActionProcessButton.Mode.ENDLESS)

        btn.setOnClickListener {
            btn.progress = 1

            val type0: String = typeTV.text.toString().toLowerCase()
            val pokemon0: String = pokeTV.text.toString().toLowerCase()

            val call1: Call<Pokemon> = pokeApiService.getPokemon(pokemon0)
            call1.enqueue(object : Callback<Pokemon> {
                override fun onFailure(call: Call<Pokemon>?, t: Throwable?) {
                    btn.progress = 0
                    Toast.makeText(this@MainActivity, "Failed to find pokemon", Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(call: Call<Pokemon>?, response: Response<Pokemon>?) {
                    val types = response?.body()?.types
                    if (types == null) {
                        btn.progress = 0
                        Toast.makeText(this@MainActivity, "Failed to find pokemon", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val call2: Call<Type> = pokeApiService.getType(type0);
                    call2.enqueue(object : Callback<Type> {
                        override fun onFailure(call: Call<Type>?, t: Throwable?) {
                            btn.progress = 0
                            Toast.makeText(this@MainActivity, "Failed to find type", Toast.LENGTH_SHORT).show()
                        }

                        override fun onResponse(call: Call<Type>?, response: Response<Type>?) {
                            val dr = response?.body()?.damageRelations
                            if (dr == null) {
                                btn.progress = 0
                                Toast.makeText(this@MainActivity, "Failed to find type", Toast.LENGTH_SHORT).show()
                                return
                            }

                            btn.progress = 100
                            Handler().postDelayed ({
                                btn.progress = 0
                            }, 2000)

                            // delay 3 seconds, then reset

                            var multiplier = 1.0
                            types.forEach {
                                multiplier *= checkEffectiveness(dr, it)
                            }

                            // Kotlin is great
                            var s = "$pokemon0 ".capitalize() + when {
                                multiplier near 0.0 -> "is not affected by"
                                multiplier near 0.25 -> "takes x1/4 damage from"
                                multiplier near 0.5 -> "takes x1/2 damage from"
                                multiplier near 1.0 -> "takes normal damage from"
                                multiplier near 2.0 -> "takes x2 damage from"
                                multiplier near 4.0 -> "takes x4 damage from"
                                else -> "takes ??? damage from"
                            } + " " + "$type0".capitalize()

                            tv.setText(tv.text.toString() + "$s \n")
                        }
                    })
                }
            })
        }


    }
}
