package com.scowluga.android.retrofitpractice

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.scowluga.android.retrofitpractice.model.*
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface PokeApiService {
    companion object {
        fun create(): PokeApiService {
            val gson: Gson = GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create()

            val retrofit: Retrofit = Retrofit.Builder()
                    .addCallAdapterFactory(
                            RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .baseUrl("http://pokeapi.co/api/v2/")
                    .build()
            return retrofit.create(PokeApiService::class.java)
        }
    }

    @GET("type/{type}/")
    fun getType(@Path("type") type: String): Call<Type>

    @GET("pokemon/{pokemon}/")
    fun getPokemon(@Path("pokemon") pokemon: String): Call<Pokemon>

    @GET("pokemon/")
    fun getAllPokemon(): Call<NamedApiResourceList>
}