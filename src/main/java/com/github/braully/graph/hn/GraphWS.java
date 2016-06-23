/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.braully.graph.hn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.commons.math3.util.CombinatoricsUtils;

/**
 * REST Web Service
 *
 * @author strike
 */
@Path("graph")
public class GraphWS {

    private static final String PARAM_NAME_HULL_NUMBER = "number";
    private static final String PARAM_NAME_HULL_SET = "set";
    private int INCLUDED = 2;
    private int NEIGHBOOR_COUNT_INCLUDED = 1;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("random")
    public UndirectedSparseGraphTO randomGraph(
            @QueryParam("nvertices") @DefaultValue("5") Integer nvertices,
            @QueryParam("minDegree") @DefaultValue("1") Integer minDegree,
            @QueryParam("maxDegree") @DefaultValue("1") Integer maxDegree) {
//        UndirectedSparseGraphTO<Integer, Integer> graph = generateRandomGraphSimple(nvertices, minDegree, maxDegree);
        UndirectedSparseGraphTO<Integer, Integer> graph = generateRandomGraph(nvertices, minDegree, maxDegree);
        return graph;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("hull")
    public Map<String, Object> calcHullNumberGraph(String jsonGraph) {
        Integer hullNumber = -1;
        Integer[] hullSet = null;

        try {
            ObjectMapper mapper = new ObjectMapper();
            BeanDeserializer bd = null;
            UndirectedSparseGraphTO<Integer, Integer> readValue = mapper.readValue(jsonGraph, UndirectedSparseGraphTO.class);
            Set<Integer> minHullSet = calcMinHullNumberGraph(readValue);
            if (minHullSet != null && !minHullSet.isEmpty()) {
                hullNumber = minHullSet.size();
                hullSet = minHullSet.toArray(new Integer[0]);
            }
        } catch (IOException ex) {
            Logger.getLogger(GraphWS.class.getName()).log(Level.SEVERE, null, ex);
        }

        /* Processar a buscar pelo hullset e hullnumber */
        Map<String, Object> response = new HashMap<>();
        response.put(PARAM_NAME_HULL_NUMBER, hullNumber);
        response.put(PARAM_NAME_HULL_SET, hullSet);

        return response;
    }

    private UndirectedSparseGraphTO<Integer, Integer> generateRandomGraphSimple(Integer nvertices,
            Integer minDegree,
            Integer maxDegree) {
        UndirectedSparseGraphTO<Integer, Integer> graph = new UndirectedSparseGraphTO<Integer, Integer>();
        Integer[] v = new Integer[nvertices];
        for (int i = 0; i < nvertices; i++) {
            v[i] = i;
            graph.addVertex(v[i]);
        }
        int countEdge = 0;
        for (int i = 0; i < nvertices; i++) {
            long limite = minDegree + Math.round(Math.random() * (maxDegree - 1));
            for (int j = 0; j <= limite; j++) {
                int vrandom = (int) Math.round(Math.random() * (nvertices - 1));
                graph.addEdge(countEdge++, v[i], v[vrandom]);
            }
        }
        return graph;
    }

    private UndirectedSparseGraphTO<Integer, Integer> generateRandomGraph(Integer nvertices,
            Integer minDegree,
            Integer maxDegree) {
        UndirectedSparseGraphTO<Integer, Integer> graph = new UndirectedSparseGraphTO<Integer, Integer>();
        List<Integer> vertexElegibles = new ArrayList<>(nvertices);
        Integer[] vertexs = new Integer[nvertices];
        int[] degree = new int[nvertices];
        for (int i = 0; i < nvertices; i++) {
            vertexElegibles.add(i);
            vertexs[i] = i;
            degree[i] = 0;
            graph.addVertex(vertexs[i]);
        }

        int countEdge = 0;
        int offset = maxDegree - minDegree - 1;
//        Integer lastVertexTarget = null;

        List<Integer> connected = new ArrayList<>();
        connected.add(vertexs[0]);
        for (int i = 1; i < nvertices; i++) {
            int vrandom = (int) Math.round(Math.random() * (connected.size() - 1));
            Integer target = connected.get(vrandom);
            Integer source = vertexs[i];
            graph.addEdge(countEdge++, source, target);
            connected.add(vertexs[i]);
            degree[target]++;
            degree[source]++;
        }

        for (int i = nvertices - 1; i > 0; i--) {
            long limite = minDegree + Math.round(Math.random() * (offset));
            int size = vertexElegibles.size();
            Integer source = vertexs[i];
            for (int j = 0; j <= limite; j++) {
                //Exclude last element from choose (no loop)
                Integer target = null;
                if (vertexElegibles.size() > 1) {
                    int vrandom = (int) Math.round(Math.random() * (size - 2));
                    target = vertexElegibles.get(vrandom);
                    if (graph.addEdge(countEdge++, source, target)) {
                        if (degree[target]++ >= maxDegree) {
                            vertexElegibles.remove(target);
                        }
                        if (degree[source]++ >= maxDegree) {
                            vertexElegibles.remove(source);
                        }
                    }
                    size = vertexElegibles.size();
                } else {
                    int vrandom = (int) Math.round(Math.random() * (nvertices - 1));
                    target = vertexs[vrandom];
                    graph.addEdge(countEdge++, source, target);
                }
//                lastVertexTarget = target;

            }
        }
        return graph;
    }

    private Set<Integer> calcMinHullNumberGraph(UndirectedSparseGraphTO<Integer, Integer> graph) {
        Set<Integer> ceilling = calcCeillingHullNumberGraph(graph);
        Set<Integer> hullSet = ceilling;
        if (graph == null || graph.getVertices().isEmpty()) {
            return ceilling;
        }
        int maxSizeSet = ceilling.size();
        int currentSize = 1;
        Collection<Integer> vertices = graph.getVertices();
        while (currentSize < maxSizeSet) {
            Set<Integer> hs = findHullSetBruteForce(graph, currentSize);
            if (hs != null && !hs.isEmpty()) {
                hullSet = hs;
                break;
            }
            currentSize++;
        }
        return hullSet;
    }

    private Set<Integer> calcCeillingHullNumberGraph(UndirectedSparseGraphTO<Integer, Integer> graph) {
        Set<Integer> ceilling = new HashSet<>();
        if (graph != null) {
            Collection<Integer> vertices = graph.getVertices();
//            int[] aux = new int[graph.getVertexCount()];
//            for (Integer i : vertices) {
//                int degree = graph.degree(i);
//                aux[i] = degree;
//                if (degree == 1) {
//                    ceilling.add(i);
//                }
//            }

            if (vertices != null) {
                ceilling.addAll(vertices);
            }
        }
        return ceilling;
    }

    public Set<Integer> findHullSetBruteForce(UndirectedSparseGraphTO<Integer, Integer> graph, int currentSetSize) {
        Set<Integer> hullSet = null;
        if (graph == null || graph.getVertexCount() <= 0) {
            return hullSet;
        }
        Collection vertices = graph.getVertices();
        Iterator<int[]> combinationsIterator = CombinatoricsUtils.combinationsIterator(graph.getVertexCount(), currentSetSize);
        while (combinationsIterator.hasNext()) {
            int[] currentSet = combinationsIterator.next();
            if (checkIfHullSet(graph, currentSet)) {
                hullSet = new HashSet<>(currentSetSize);
                for (int i : currentSet) {
                    hullSet.add(i);
                }
                break;
            }
        }
        return hullSet;
    }

    public boolean checkIfHullSet(UndirectedSparseGraphTO<Integer, Integer> graph,
            int[] currentSet) {
        if (currentSet == null || currentSet.length == 0) {
            return false;
        }
        Set<Integer> fecho = new HashSet<>();
        Collection vertices = graph.getVertices();
        int[] aux = new int[graph.getVertexCount()];
        for (int i = 0; i < aux.length; i++) {
            aux[i] = 0;
        }
        for (int i : currentSet) {
            includeVertex(graph, fecho, aux, i);
        }
        return fecho.size() == graph.getVertexCount();
    }

    public void includeVertex(UndirectedSparseGraphTO<Integer, Integer> graph, Set<Integer> fecho, int[] aux, int i) {
        fecho.add(i);
        aux[i] = INCLUDED;
        Collection<Integer> neighbors = graph.getNeighbors(i);
        for (int vert : neighbors) {
            if (vert != i) {
                int previousValue = aux[vert];
                aux[vert] = aux[vert] + NEIGHBOOR_COUNT_INCLUDED;
                if (previousValue < INCLUDED && aux[vert] >= INCLUDED) {
                    includeVertex(graph, fecho, aux, vert);
                }
            }
        }
    }
}
