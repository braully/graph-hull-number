/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.braully.graph.hn;

/**
 *
 * @author strike
 */
//https://msdn.microsoft.com/en-us/library/aa289166(v=vs.71).aspx
public class Combination {

    private long n = 0;
    private int k = 0;
    private long[] data = null;

    public Combination(long n, int k) {
        if (n < 0 || k < 0) // normally n >= k
        {
            throw new RuntimeException("Negative parameter in constructor");
        }

        this.n = n;
        this.k = k;
        this.data = new long[k];
        for (int i = 0; i < k; ++i) {
            this.data[i] = i;
        }
    } // Combination(n,k)

    public Combination(long n, int k, long[] a) // Combination from a[]
    {
        if (k != a.length) {
            throw new IllegalArgumentException("Array length does not equal k");
        }

        this.n = n;
        this.k = k;
        this.data = new long[k];
        for (int i = 0; i < a.length; ++i) {
            this.data[i] = a[i];
        }

        if (!this.IsValid()) {
            throw new IllegalArgumentException("Bad value from array");
        }
    } // Combination(n,k,a)

    public boolean IsValid() {
        if (this.data.length != this.k) {
            return false; // corrupted
        }
        for (int i = 0; i < this.k; ++i) {
            if (this.data[i] < 0 || this.data[i] > this.n - 1) {
                return false; // value out of range
            }
            for (int j = i + 1; j < this.k; ++j) {
                if (this.data[i] >= this.data[j]) {
                    return false;
                }
            }
        }

        return true;
    } // IsValid()

    public String toString() {
        String s = "{ ";
        for (int i = 0; i < this.k; ++i) {
            s += this.data[i] + " ";
        }
        s += "}";
        return s;
    } // ToString()

    public Combination Successor() {
        if (this.data[0] == this.n - this.k) {
            return null;
        }
        Combination ans = new Combination(this.n, this.k);
        int i;
        for (i = 0; i < this.k; ++i) {
            ans.data[i] = this.data[i];
        }

        for (i = this.k - 1; i > 0 && ans.data[i] == this.n - this.k + i; --i);

        ++ans.data[i];

        for (int j = i; j < this.k - 1; ++j) {
            ans.data[j + 1] = ans.data[j] + 1;
        }

        return ans;
    } // Successor()

    public static long Choose(long n, int k) {
        if (n < 0 || k < 0) {
            throw new IllegalArgumentException("Invalid negative parameter in Choose()");
        }
        if (n < k) {
            return 0;  // special case
        }
        if (n == k) {
            return 1;
        }

        long delta, iMax;

        if (k < n - k) // ex: Choose(100,3)
        {
            delta = n - k;
            iMax = k;
        } else // ex: Choose(100,97)
        {
            delta = k;
            iMax = n - k;
        }

        long ans = delta + 1;

        for (long i = 2; i <= iMax; ++i) {
            try {
                ans = (ans * (delta + i)) / i;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return ans;
    } // Choose()

    public Combination Element(long m) {
        long[] ans = new long[this.k];
        long a = this.n;
        int b = this.k;
        long x = (Choose(this.n, this.k) - 1) - m; // x is the "dual" of m
        for (int i = 0; i < this.k; ++i) {
            ans[i] = LargestV(a, b, x); // largest value v, where v < a and vCb < x    
            x = x - Choose(ans[i], b);
            a = ans[i];
            b = b - 1;
        }

        for (int i = 0; i < this.k; ++i) {
            ans[i] = (n - 1) - ans[i];
        }

        return new Combination(this.n, this.k, ans);
    } // Element()

// return largest value v where v < a and  Choose(v,b) <= x
    private static long LargestV(long a, int b, long x) {
        long v = a - 1;

        while (Choose(v, b) > x) {
            --v;
        }

        return v;
    } // LargestV()
}
