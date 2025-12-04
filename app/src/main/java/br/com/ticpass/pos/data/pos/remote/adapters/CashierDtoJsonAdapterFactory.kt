package br.com.ticpass.pos.data.pos.remote.adapters

import br.com.ticpass.pos.data.pos.remote.dto.CashierDto
import com.squareup.moshi.*
import java.lang.reflect.Type

object CashierDtoJsonAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (type != CashierDto::class.java) return null

        val delegate: JsonAdapter<CashierDto> = moshi.nextAdapter(this, type, annotations)

        return object : JsonAdapter<CashierDto?>() {
            override fun fromJson(reader: JsonReader): CashierDto? {
                return when (reader.peek()) {
                    JsonReader.Token.BEGIN_OBJECT -> {
                        delegate.fromJson(reader)
                    }
                    JsonReader.Token.STRING -> {
                        val id = reader.nextString()
                        CashierDto(
                            id = id,
                            avatar = null,
                            username = null,
                            name = null,
                            email = null,
                            role = null,
                            totp = null,
                            managers = emptyList(),
                            oauth2 = emptyList(),
                            createdBy = null,
                            createdAt = null,
                            updatedAt = null
                        )
                    }
                    JsonReader.Token.NULL -> {
                        reader.nextNull<Unit>()
                        null
                    }
                    else -> throw JsonDataException("Unexpected token for cashier: ${reader.peek()}")
                }
            }

            override fun toJson(writer: JsonWriter, value: CashierDto?) {
                delegate.toJson(writer, value)
            }
        }
    }
}