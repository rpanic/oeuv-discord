package db

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import java.io.File
import java.util.*
import kotlin.reflect.typeOf

@OptIn(ExperimentalSerializationApi::class)
object BotDB {

    val emailStatus = ObservableList<EmailStatus>(mutableListOf(), "emails")

    val baseFile = File("./data/")

    var initialized = false

    fun init(){

        if(initialized) throw RuntimeException("Already initialized")

        if(!baseFile.exists()){
            baseFile.mkdir()
        }

        val stores = listOf(
            emailStatus
        )

        stores.forEach {

            parseJsonToList(it)

            it.addObserver { o, _ ->
                saveList(it)
            }
        }

        initialized = true

    }

    inline fun <reified T: Any> parseJsonToList(list: ObservableList<T>){

        val file = baseFile.resolve("${list.name}.json")
        if(file.exists()){
            val d: DeserializationStrategy<List<T>> = serializer<List<T>>()
            val text = file.readText()
            val parsed = Json.decodeFromString<List<T>>(d, text)
            list.getWrapped().addAll(parsed) //On purpose not observable-triggering
        }else{
            file.createNewFile()
            file.writeText("[]")
        }

    }

    inline fun <reified T: Any> saveList(list: ObservableList<T>){

        Json.encodeToStream(serializer(typeOf<List<T>>()), list.getWrapped(), baseFile.resolve("${list.name}.json").outputStream())

    }

}

@Serializable
data class EmailStatus(
    val id: Long,
    val received: Long,
    var status: Int
)

class ObservableList<T>(private val wrapped: MutableList<T>, val name: String): MutableList<T> by wrapped, Observable() {
    override fun add(element: T): Boolean {
        if (wrapped.add(element)) {
            setChanged()
            notifyObservers()
            return true
        }
        return false
    }

    override fun remove(element: T): Boolean {
        if(wrapped.remove(element)){
            setChanged()
            notifyObservers()
            return true
        }
        return false
    }

    fun getWrapped() = wrapped

    fun changed() {
        setChanged()
        notifyObservers()
    }
}