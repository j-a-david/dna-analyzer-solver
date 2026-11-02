/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Derived from the DNA Analyzer solver in SkyHanni
 * (https://github.com/hannibal002/SkyHanni), rewritten in Java.
 */
package dev.dnasolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pure solving logic with no Minecraft dependencies.
 *
 * The board is int[rows][4]. Each row holds the four colours of one vertical
 * strand (values 0..3, all distinct within a row). A move swaps two positions
 * inside a single row. Neighbouring rows must "connect": every colour in a row
 * must appear in the next row at the same position or one position above/below.
 */
public final class DnaSolver {

    private static final int INF = 1000;
    private static final int[][] PERMS = generatePermutations();

    private DnaSolver() {
    }

    /** Swap positions a and b inside board row {@code row}. */
    public record Swap(int row, int a, int b) {
    }

    public record Solution(int swapsNeeded, List<Swap> swaps) {
    }

    /** Returns null if the board has no valid solution. */
    public static Solution solve(int[][] board, boolean allowEnds) {
        int n = board.length;
        if (n < 3) {
            return null;
        }
        int first = allowEnds ? 0 : 1;
        int last = allowEnds ? n - 1 : n - 2;
        int m = last - first + 1;
        int p = PERMS.length;

        int[][] cost = new int[m][p];
        int[][][] arr = new int[m][p][];
        for (int i = 0; i < m; i++) {
            for (int q = 0; q < p; q++) {
                arr[i][q] = arrange(board[first + i], PERMS[q]);
                cost[i][q] = minSwaps(board[first + i], arr[i][q]).size();
            }
        }

        int[][] dp = new int[m][p];
        int[][] parent = new int[m][p];
        for (int i = 0; i < m; i++) {
            for (int q = 0; q < p; q++) {
                dp[i][q] = INF;
                parent[i][q] = -1;
            }
        }

        for (int q = 0; q < p; q++) {
            if (allowEnds || connects(board[0], arr[0][q])) {
                dp[0][q] = cost[0][q];
            }
        }
        for (int i = 1; i < m; i++) {
            for (int q = 0; q < p; q++) {
                for (int prev = 0; prev < p; prev++) {
                    if (dp[i - 1][prev] == INF) {
                        continue;
                    }
                    if (!connects(arr[i - 1][prev], arr[i][q])) {
                        continue;
                    }
                    int total = dp[i - 1][prev] + cost[i][q];
                    if (total < dp[i][q]) {
                        dp[i][q] = total;
                        parent[i][q] = prev;
                    }
                }
            }
        }

        int best = INF;
        int bestPerm = -1;
        for (int q = 0; q < p; q++) {
            if (!(allowEnds || connects(arr[m - 1][q], board[n - 1]))) {
                continue;
            }
            if (dp[m - 1][q] < best) {
                best = dp[m - 1][q];
                bestPerm = q;
            }
        }
        if (bestPerm == -1) {
            return null;
        }

        List<Swap> swaps = new ArrayList<>();
        int cur = bestPerm;
        for (int i = m - 1; i >= 0; i--) {
            int row = first + i;
            List<int[]> rowSwaps = minSwaps(board[row], arr[i][cur]);
            for (int k = rowSwaps.size() - 1; k >= 0; k--) {
                int[] s = rowSwaps.get(k);
                swaps.add(new Swap(row, s[0], s[1]));
            }
            cur = parent[i][cur];
        }
        Collections.reverse(swaps);
        return new Solution(best, swaps);
    }

    static boolean connects(int[] a, int[] b) {
        for (int r = 0; r < 4; r++) {
            int v = a[r];
            boolean ok = b[r] == v || (r > 0 && b[r - 1] == v) || (r < 3 && b[r + 1] == v);
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static int[] arrange(int[] row, int[] perm) {
        int[] out = new int[4];
        for (int k = 0; k < 4; k++) {
            out[k] = row[perm[k]];
        }
        return out;
    }

    /** Minimum swaps (as index pairs) turning {@code from} into {@code to}. Colours are distinct. */
    static List<int[]> minSwaps(int[] from, int[] to) {
        int[] pos = new int[4];
        for (int i = 0; i < 4; i++) {
            int src = indexOf(from, to[i]);
            pos[src] = i;
        }
        boolean[] visited = new boolean[4];
        List<int[]> swaps = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            if (visited[i]) {
                continue;
            }
            List<Integer> cycle = new ArrayList<>();
            int cur = i;
            while (!visited[cur]) {
                visited[cur] = true;
                cycle.add(cur);
                cur = pos[cur];
            }
            for (int k = 1; k < cycle.size(); k++) {
                swaps.add(new int[]{cycle.get(0), cycle.get(k)});
            }
        }
        return swaps;
    }

    private static int indexOf(int[] a, int v) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] == v) {
                return i;
            }
        }
        throw new IllegalArgumentException("colour missing from row");
    }

    private static int[][] generatePermutations() {
        List<int[]> out = new ArrayList<>();
        permute(new int[]{0, 1, 2, 3}, 0, out);
        return out.toArray(new int[0][]);
    }

    private static void permute(int[] a, int l, List<int[]> out) {
        if (l == a.length) {
            out.add(a.clone());
            return;
        }
        for (int i = l; i < a.length; i++) {
            swap(a, i, l);
            permute(a, l + 1, out);
            swap(a, i, l);
        }
    }

    private static void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}
