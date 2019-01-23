package com.scowluga.android.pokemon

import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.*
import com.dd.processbutton.iml.ActionProcessButton
import com.scowluga.android.pokemon.model.*
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    val pokeApiService by lazy {
        // Singleton design pattern
        PokeApiService.create()
    }

    // effectiveness of the TypeRelations against a specific type
    fun checkEffectiveness(dr: TypeRelations, type: PokemonType) = when {
        dr.noDamageTo.any { it same type } -> 0.0
        dr.halfDamageTo.any { it same type } -> 0.5
        dr.doubleDamageTo.any { it same type } -> 2.0
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

        val typeAdapter = ArrayAdapter<String>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                typeList
        )

        typeTV.setAdapter(typeAdapter)
        typeTV.threshold = 1


        // Initializing Pokemon AutoComplete
        val pokeTV: TextInputAutoCompleteTextView = findViewById(R.id.atv2)

        pokeApiService.getAllPokemon()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map { it.results }
                .subscribe(object : Observer<List<NamedApiResource>> {
                    override fun onSubscribe(d: Disposable) {
                        Log.d("TAG", "onSubscribe")
                    }

                    override fun onError(e: Throwable) {
                        Toast.makeText(this@MainActivity, "Failed to load Pokemon", Toast.LENGTH_SHORT).show()
                        Log.d("TAG", "Search failed")
                    }

                    override fun onComplete() {
                        Log.d("TAG", "onComplete")
                    }

                    override fun onNext(t: List<NamedApiResource>) {

                        val pokeAdapter = ArrayAdapter<String>(
                                this@MainActivity,
                                android.R.layout.simple_dropdown_item_1line,
                                t.map { it.name.capitalize() } // wow Kotlin is powerful
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

            val typeCall = pokeApiService.getType(type0)
            val pokeCall = pokeApiService.getPokemon(pokemon0)

            Observable.zip(typeCall, pokeCall,
                    BiFunction<Type, Pokemon, Pair<Type, Pokemon>>
                    { t: Type, p: Pokemon -> Pair(t, p) })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(object : Observer<Pair<Type, Pokemon>> {
                        override fun onSubscribe(d: Disposable) {
                            Log.d("TAG", "onSubscribe")
                        }

                        override fun onError(e: Throwable) {
                            Toast.makeText(this@MainActivity, "Search Failed", Toast.LENGTH_SHORT).show()
                            btn.progress = 0
                            Log.d("TAG", "Search failed")
                        }

                        override fun onComplete() {
                            Log.d("TAG", "onComplete")
                        }

                        override fun onNext(t: Pair<Type, Pokemon>) {
                            val (type, pokemon) = t
                            val dr = type.damageRelations // damange relations
                            val pt = pokemon.types // pokemon types

                            btn.progress = 100
                            Handler().postDelayed({
                                btn.progress = 0
                            }, 2500)

                            var multiplier = 1.0
                            pt.forEach {
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
    }
}
