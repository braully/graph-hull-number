/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.braully.graph.hn;

import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;
import java.util.Collection;

/**
 *
 * @author strike
 */
public class UndirectedSparseGraphTO<V, E> extends UndirectedSparseGraph {

    public Collection<Pair<V>> pairs = null;

    public Collection<Pair<V>> getPairs() {
        Collection values = this.edges.values();
        return (Collection<Pair<V>>) values;
    }
}
