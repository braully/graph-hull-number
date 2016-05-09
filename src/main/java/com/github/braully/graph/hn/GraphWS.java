/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.braully.graph.hn;

import com.github.braully.graph.hn.UndirectedSparseGraphTO;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST Web Service
 *
 * @author strike
 */
@Path("graph")
public class GraphWS {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("random")
    public Response randomGraph(
            @QueryParam("nvertices") Integer nvertices,
            @QueryParam("minDegree") Integer minDegree,
            @QueryParam("maxDegree") Integer maxDegree) {
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
        return Response.ok(graph).build();
    }
}
