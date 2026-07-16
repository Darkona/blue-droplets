package com.darkona.droplets.content.data;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.Codec;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Optional fields that fail on an invalid value instead of reading it as absent, like {@code optionalFieldOf} in the
 * codecs of later versions; the {@code optionalFieldOf} of Minecraft 1.20.1 hides the error.
 */
final class StrictFields
{
    private StrictFields() {}

    static <A> MapCodec<Optional<A>> optional(Codec<A> codec, String name)
    {
        return new MapCodec<>()
        {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops)
            {
                return Stream.of(ops.createString(name));
            }

            @Override
            public <T> DataResult<Optional<A>> decode(DynamicOps<T> ops, MapLike<T> input)
            {
                T value = input.get(name);
                return value == null ? DataResult.success(Optional.empty()) : codec.parse(ops, value).map(Optional::of);
            }

            @Override
            public <T> RecordBuilder<T> encode(Optional<A> input, DynamicOps<T> ops, RecordBuilder<T> prefix)
            {
                return input.isPresent() ? prefix.add(name, codec.encodeStart(ops, input.get())) : prefix;
            }
        };
    }

    static <A> MapCodec<A> optional(Codec<A> codec, String name, A fallback)
    {
        return optional(codec, name).xmap(value -> value.orElse(fallback), value -> Objects.equals(value, fallback) ? Optional.empty() : Optional.of(value));
    }
}
