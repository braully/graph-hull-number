/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.braully.graph.hn;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final Logger log = Logger.getLogger(GraphWS.class.getSimpleName());

    private static final String PARAM_NAME_HULL_NUMBER = "number";
    private static final String PARAM_NAME_HULL_SET = "set";
    private static final String PARAM_NAME_SERIAL_TIME = "serial";
    private static final String PARAM_NAME_PARALLEL_TIME = "parallel";
    private static final String COMMAND_GRAPH_HN = "/home/strike/Workspace/pesquisa/graph-hull-number-parallel/graph-test/";
    private final int INCLUDED = 2;
    private final int NEIGHBOOR_COUNT_INCLUDED = 1;

    private static final Pattern PATERN_HULL_SET = Pattern.compile(".*?Combination: \\{([0-9, ]+)\\}.*?");
    private static final Pattern PATERN_HULL_NUMBER = Pattern.compile(".*?S\\| = ([0-9]+).*?");
    private static final Pattern PATERN_SERIAL_TIME = Pattern.compile("Total time serial: (\\w+)");
    private static final Pattern PATERN_PARALLEL_TIME = Pattern.compile("Total time parallel: (\\w+)");

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("random")
    public UndirectedSparseGraphTO randomGraph(
            @QueryParam("nvertices") @DefaultValue("5") Integer nvertices,
            @QueryParam("minDegree") @DefaultValue("1") Integer minDegree,
            @QueryParam("maxDegree") @DefaultValue("1") Double maxDegree) {
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
            log.log(Level.SEVERE, null, ex);
        }

        /* Processar a buscar pelo hullset e hullnumber */
        Map<String, Object> response = new HashMap<>();
        response.put(PARAM_NAME_HULL_NUMBER, hullNumber);
        response.put(PARAM_NAME_HULL_SET, hullSet);
        response.put(PARAM_NAME_PARALLEL_TIME, "--");
        response.put(PARAM_NAME_SERIAL_TIME, "--");
        return response;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("hullparallel")
    public Map<String, Object> calcHullNumberGraphParallel(String jsonGraph) {
        Integer hullNumber = null;
        Integer[] hullSet = null;
        String pTime = null;
        String sTime = null;

        try {
            ObjectMapper mapper = new ObjectMapper();
            BeanDeserializer bd = null;
            UndirectedSparseGraphTO<Integer, Integer> undGraph = mapper.readValue(jsonGraph, UndirectedSparseGraphTO.class);

            String path = saveTmpFileGraphInCsr(undGraph);

            String commandToExecute = COMMAND_GRAPH_HN + " -sp " + path;

            log.log(Level.INFO, "Command: {0}", commandToExecute);
            log.log(Level.INFO, "Executing");
            Process p = Runtime.getRuntime().exec(commandToExecute);
            p.waitFor();
            log.log(Level.INFO, "Executed");
            BufferedReader reader
                    = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";
            log.log(Level.INFO, "Output");
            while ((line = reader.readLine()) != null) {
                log.log(Level.INFO, line);
                try {
                    if (hullSet == null) {
                        hullSet = parseHullSet(line);
                    }
                    if (hullNumber == null) {
                        hullNumber = parseHullNumber(line);
                    }
                    if (pTime == null) {
                        pTime = parseParallelTime(line);
                    }
                    if (sTime == null) {
                        sTime = parseSerialTime(line);
                    }
                } catch (Exception e) {
                    log.log(Level.WARNING, "", e);
                }
            }

        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            log.log(Level.SEVERE, null, ex);
        }

        Map<String, Object> response = new HashMap<>();
        response.put(PARAM_NAME_HULL_NUMBER, hullNumber);
        response.put(PARAM_NAME_HULL_SET, hullSet);
        response.put(PARAM_NAME_PARALLEL_TIME, pTime);
        response.put(PARAM_NAME_SERIAL_TIME, sTime);
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
            Double maxDegree) {
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
        long extimatedEdge = nvertices * minDegree + Math.round(nvertices * (maxDegree - minDegree));

        int countEdge = 0;
        Double offset = maxDegree - minDegree - 1;
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

        if (offset > 1) {
            for (int i = nvertices - 1; i > 0; i--) {
                long limite = minDegree + Math.round(Math.random() * offset);
                int size = vertexElegibles.size();
                Integer source = vertexs[i];
                for (int j = 0; j <= limite; j++) {
                    //Exclude last element from choose (no loop)
                    Integer target = null;
                    if (vertexElegibles.size() > 1) {
                        int vrandom = (int) Math.round(Math.random() * (size - 1));
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
                        if (graph.addEdge(countEdge++, source, target)) {

                        }
                    }
//                lastVertexTarget = target;

                }
            }
        }

        int edgeCount = graph.getEdgeCount();

        if (edgeCount < extimatedEdge) {
            for (int i = 0; i < extimatedEdge - edgeCount; i++) {
                int vrandom = (int) Math.round(Math.random() * (nvertices - 1));
                while (!graph.addEdge(countEdge++, vrandom, (int) Math.round(Math.random() * (nvertices - 1))));
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

        Queue<Integer> mustBeIncluded = new ArrayDeque<>();
        for (Integer v : currentSet) {
            mustBeIncluded.add(v);
        }
        while (!mustBeIncluded.isEmpty()) {
            Integer verti = mustBeIncluded.remove();
            fecho.add(verti);
            aux[verti] = INCLUDED;
            Collection<Integer> neighbors = graph.getNeighbors(verti);
            for (int vertn : neighbors) {
                if (vertn != verti) {
                    int previousValue = aux[vertn];
                    aux[vertn] = aux[vertn] + NEIGHBOOR_COUNT_INCLUDED;
                    if (previousValue < INCLUDED && aux[vertn] >= INCLUDED) {
//                        includeVertex(graph, fecho, aux, verti);
                        mustBeIncluded.add(vertn);
                    }
                }
            }
        }

//        for (int i : currentSet) {
//            includeVertex(graph, fecho, aux, i);
//        }
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

    private String saveTmpFileGraphInCsr(UndirectedSparseGraphTO<Integer, Integer> undGraph) {
        String strFile = null;
        if (undGraph != null && undGraph.getVertexCount() > 0) {
            try {
                int vertexCount = undGraph.getVertexCount();
                int edegeCount = undGraph.getEdgeCount();
                File file = new File(COMMAND_GRAPH_HN + "graph-csr-" + vertexCount + "-" + edegeCount + ".txt");
//                file.deleteOnExit();

                strFile = file.getAbsolutePath();
                FileWriter writer = new FileWriter(file);
                writer.write("#Graph |V| = " + vertexCount + "\n");

                int sizeRowOffset = 0;
                List<Integer> csrColIdxs = new ArrayList<>();
                List<Integer> rowOffset = new ArrayList<>();

                int idx = 0;
                for (Integer i = 0; i < vertexCount; i++) {
                    csrColIdxs.add(idx);
                    Collection<Integer> neighbors = undGraph.getNeighbors(i);
                    Set<Integer> neighSet = new HashSet<>();
                    neighSet.addAll(neighbors);
                    for (Integer vn : neighSet) {
                        if (!vn.equals(i)) {
                            rowOffset.add(vn);
                            idx++;
                        }
                    }
                }
                csrColIdxs.add(idx);

                for (Integer i : csrColIdxs) {
                    writer.write("" + i);
                    writer.write(" ");
                }
                writer.write("\n");
                for (Integer i : rowOffset) {
                    writer.write("" + i);
                    writer.write(" ");
                }
                writer.write("\n");
                writer.close();
            } catch (IOException ex) {
                log.log(Level.SEVERE, null, ex);
            }
        }
        log.log(Level.INFO, "File tmp graph: {0}", strFile);
        return strFile;
    }

    private Integer[] parseHullSet(String line) {
        Integer[] ret = null;
        Matcher m = PATERN_HULL_SET.matcher(line);
        if (m.find()) {
            String[] split = m.group(1).split(",");
            if (split != null && split.length > 0) {
                ret = new Integer[split.length];
                for (int i = 0; i < split.length; i++) {
                    String st = split[i];
                    ret[i] = Integer.parseInt(st.trim());
                }
            }
        }
        return ret;
    }

    private Integer parseHullNumber(String line) {
        Integer ret = null;
        Matcher m = PATERN_HULL_NUMBER.matcher(line);
        if (m.find()) {
            String trim = m.group();
            trim = m.group(1);
//            String trim = m.group();
            if (trim != null && !trim.isEmpty()) {
                ret = Integer.parseInt(trim.trim());
            }
        }
        return ret;
    }

    private String parseParallelTime(String line) {
        String ret = null;
        Matcher m = PATERN_PARALLEL_TIME.matcher(line);
        if (m.find()) {
            ret = m.group(1);
        }
        return ret;
    }

    private String parseSerialTime(String line) {
        String ret = null;
        Matcher m = PATERN_SERIAL_TIME.matcher(line);
        if (m.find()) {
            ret = m.group(1);
        }
        return ret;
    }
}
