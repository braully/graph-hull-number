/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.braully.graph.hn;

import java.util.NoSuchElementException;
import junit.framework.TestCase;

/**
 *
 * @author strike
 */
public class CombintionTest extends TestCase {

    private int k;//final
    private int[] c;//final
    private boolean more = true;
    private int j;

    public CombintionTest() {
    }

    public CombintionTest(String testName) {
        super(testName);
    }

    public void testCombination() {
        int n = 10;
        int k = 3;

        //init
        this.k = k;
        c = new int[k + 3];
        if (k == 0 || k >= n) {
            more = false;
            return;
        }
        for (int i = 1; i <= k; i++) {
            c[i] = i - 1;
        }
        c[k + 1] = n;
        c[k + 2] = 0;
        j = k;

        long i = 1;
        //combinations
        while (more) {
            int[] next = next();
            System.err.printf("%3d - %d: ", i++, j);
            for (int x : next) {
                System.out.printf("%3d ", x);
            }
            System.out.println();
        }
    }

    public int[] next() {
        if (!more) {
            throw new NoSuchElementException();
        }
        // Copy return value (prepared by last activation)
        final int[] ret = new int[k];
        System.arraycopy(c, 1, ret, 0, k);

        // Prepare next iteration
        // T2 and T6 loop
        int x = 0;
        if (j > 0) {
            x = j;
            c[j] = x;
            j--;
            return ret;
        }
        // T3
        if (c[1] + 1 < c[2]) {
            c[1]++;
            return ret;
        } else {
            j = 2;
        }
        // T4
        boolean stepDone = false;
        while (!stepDone) {
            c[j - 1] = j - 2;
            x = c[j] + 1;
            if (x == c[j + 1]) {
                j++;
            } else {
                stepDone = true;
            }
        }
        // T5
        if (j > k) {
            more = false;
            return ret;
        }
        // T6
        c[j] = x;
        j--;
        return ret;
    }

    public void testCombinadic() {
        Combination c = new Combination(25, 14);
        Combination Element = c.Element(1000);
        System.err.println("C100(25, 14): " + Element);
    }
}
