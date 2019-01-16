package com.scowluga.android.retrofitpractice

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.*
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.scowluga.android.retrofitpractice.model.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

class MainActivity : AppCompatActivity() {

    val pokeApiService by lazy {
        PokeApiService.create()
    }

    // effectiveness of the TypeRelations against a specific type
    fun checkEffectiveness(dr: TypeRelations, type: PokemonType): Double {
        if (dr.noDamageTo.any {it same type})
            return 0.0
        if (dr.halfDamageTo.any {it same type})
            return 0.5
        if (dr.doubleDamageTo.any {it same type})
            return 2.0
        return 1.0
    }

    infix fun Double.near(d: Double): Boolean = (this - d) < 0.001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initializing Type AutoComplete
        val typeTV: AutoCompleteTextView = findViewById(R.id.atv1)
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
        val pokeTV: AutoCompleteTextView = findViewById(R.id.atv2)

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
                        pokeList.map { it.name }
                )

                pokeTV.setAdapter(pokeAdapter)
                pokeTV.threshold = 1
            }
        })

        val btn: Button = findViewById(R.id.btn)
        btn.setOnClickListener {

            val type0: String = typeTV.text.toString().toLowerCase()
            val pokemon0: String = pokeTV.text.toString().toLowerCase()

            val call1: Call<Pokemon> = pokeApiService.getPokemon(pokemon0)
            call1.enqueue(object : Callback<Pokemon> {
                override fun onFailure(call: Call<Pokemon>?, t: Throwable?) {
                    Toast.makeText(this@MainActivity, "Failed to find pokemon", Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(call: Call<Pokemon>?, response: Response<Pokemon>?) {
                    val types = response?.body()?.types
                    if (types == null) {
                        Toast.makeText(this@MainActivity, "Failed to find pokemon", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val call2: Call<Type> = pokeApiService.getType(type0);
                    call2.enqueue(object : Callback<Type> {
                        override fun onFailure(call: Call<Type>?, t: Throwable?) {
                            Toast.makeText(this@MainActivity, "Failed to find type", Toast.LENGTH_SHORT).show()
                        }

                        override fun onResponse(call: Call<Type>?, response: Response<Type>?) {
                            val dr = response?.body()?.damageRelations
                            if (dr == null) {
                                Toast.makeText(this@MainActivity, "Failed to find type", Toast.LENGTH_SHORT).show()
                                return
                            }

                            var multiplier = 1.0
                            types.forEach {
                                multiplier *= checkEffectiveness(dr, it)
                            }

                            when {
                                multiplier near 0.0 ->
                                    Toast.makeText(this@MainActivity, "Doesn't Effect", Toast.LENGTH_SHORT).show()
                                multiplier near 0.25 ->
                                    Toast.makeText(this@MainActivity, "x1/4", Toast.LENGTH_SHORT).show()
                                multiplier near 0.5 ->
                                    Toast.makeText(this@MainActivity, "x1/2", Toast.LENGTH_SHORT).show()
                                multiplier near 1.0 ->
                                    Toast.makeText(this@MainActivity, "x1", Toast.LENGTH_SHORT).show()
                                multiplier near 2.0 ->
                                    Toast.makeText(this@MainActivity, "x2", Toast.LENGTH_SHORT).show()
                                multiplier near 4.0 ->
                                    Toast.makeText(this@MainActivity, "x4", Toast.LENGTH_SHORT).show()

                            }

                        }
                    })
                }
            })
        }


    }
}
