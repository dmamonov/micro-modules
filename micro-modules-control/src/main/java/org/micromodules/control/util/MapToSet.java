package org.micromodules.control.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author dmitry.mamonov
 *         Created: 2014-12-29 3:58 PM
 */
public class MapToSet<K, V> extends LinkedHashMap<K, Set<V>> {
    @Override
    public Set<V> get(final Object key) {
        final Set<V> value = super.get(key);
        if (value!=null){
            return value;
        } else {
            final Set<V> newValue = new LinkedHashSet<V>(){
                @Override
                public boolean add(final V o) {
                    //noinspection SimplifiableIfStatement
                    if (o!=null) {
                        return super.add(o);
                    } else {
                        return false;
                    }
                }
            };
            //noinspection unchecked
            super.put((K)key, newValue);
            return newValue;
        }
    }

    public ImmutableMap<K, ImmutableSet<V>> convertToImmutableMap(){
        final ImmutableMap.Builder<K, ImmutableSet<V>> resultBuilder = ImmutableMap.builder();
        entrySet().forEach(entry -> resultBuilder.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue())));
        return resultBuilder.build();
    }
}
