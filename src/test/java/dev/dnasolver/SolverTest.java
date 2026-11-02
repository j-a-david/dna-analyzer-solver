package dev.dnasolver;

import java.util.*;

public final class SolverTest {

    static boolean connects(int[] a, int[] b) {
        for (int r = 0; r < 4; r++) {
            int v = a[r];
            boolean ok = b[r] == v || (r > 0 && b[r - 1] == v) || (r < 3 && b[r + 1] == v);
            if (!ok) return false;
        }
        return true;
    }

    // Independent oracle: transposition distance via BFS over 24 permutations.
    static int[][] dist = new int[24][24];
    static List<int[]> perms = new ArrayList<>();

    static void gen(int[] a, int l) {
        if (l == 4) { perms.add(a.clone()); return; }
        for (int i = l; i < 4; i++) {
            int t = a[i]; a[i] = a[l]; a[l] = t;
            gen(a, l + 1);
            t = a[i]; a[i] = a[l]; a[l] = t;
        }
    }

    static int idx(int[] p) {
        for (int i = 0; i < perms.size(); i++) if (Arrays.equals(perms.get(i), p)) return i;
        throw new IllegalStateException();
    }

    static void buildDist() {
        gen(new int[]{0, 1, 2, 3}, 0);
        for (int s = 0; s < 24; s++) {
            Arrays.fill(dist[s], -1);
            dist[s][s] = 0;
            Deque<Integer> q = new ArrayDeque<>();
            q.add(s);
            while (!q.isEmpty()) {
                int c = q.poll();
                for (int i = 0; i < 4; i++) for (int j = i + 1; j < 4; j++) {
                    int[] p = perms.get(c).clone();
                    int t = p[i]; p[i] = p[j]; p[j] = t;
                    int n = idx(p);
                    if (dist[s][n] < 0) { dist[s][n] = dist[s][c] + 1; q.add(n); }
                }
            }
        }
    }

    // Brute force optimum for a board of n rows (n small) by enumerating every arrangement.
    static int brute(int[][] board, boolean allowEnds) {
        int n = board.length;
        int first = allowEnds ? 0 : 1, last = allowEnds ? n - 1 : n - 2;
        int m = last - first + 1;
        int best = Integer.MAX_VALUE;
        int[] choice = new int[m];
        int total = (int) Math.pow(24, m);
        for (int code = 0; code < total; code++) {
            int c = code;
            for (int i = 0; i < m; i++) { choice[i] = c % 24; c /= 24; }
            int[][] b = new int[n][];
            for (int r = 0; r < n; r++) b[r] = board[r].clone();
            int cost = 0;
            for (int i = 0; i < m; i++) {
                int[] p = perms.get(choice[i]);
                int[] row = new int[4];
                for (int k = 0; k < 4; k++) row[k] = board[first + i][p[k]];
                // distance between original row and arranged row, as permutation of positions
                int[] mapping = new int[4];
                for (int k = 0; k < 4; k++) {
                    for (int s = 0; s < 4; s++) if (board[first + i][s] == row[k]) mapping[k] = s;
                }
                cost += dist[idx(new int[]{0, 1, 2, 3})][idx(mapping)];
                b[first + i] = row;
            }
            boolean ok = true;
            for (int r = 0; r + 1 < n && ok; r++) ok = connects(b[r], b[r + 1]);
            if (ok) best = Math.min(best, cost);
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    static int[] randomRow(Random rnd) {
        List<Integer> l = new ArrayList<>(List.of(0, 1, 2, 3));
        Collections.shuffle(l, rnd);
        return new int[]{l.get(0), l.get(1), l.get(2), l.get(3)};
    }

    public static void main(String[] args) {
        buildDist();
        Random rnd = new Random(12345);
        int solved = 0, unsolvable = 0, checkedOptimal = 0;

        for (int iter = 0; iter < 4000; iter++) {
            int n = (iter % 3 == 0) ? 4 : 9;
            boolean allowEnds = iter % 2 == 0;
            int[][] board = new int[n][];
            // Bias toward solvable boards: start from a connected chain then scramble rows.
            for (int r = 0; r < n; r++) board[r] = randomRow(rnd);
            if (iter % 4 != 0) {
                int[][] chain = new int[n][];
                chain[0] = randomRow(rnd);
                for (int r = 1; r < n; r++) {
                    int[] cand;
                    int guard = 0;
                    do { cand = randomRow(rnd); guard++; } while (!connects(chain[r - 1], cand) && guard < 500);
                    chain[r] = cand;
                }
                boolean ok = true;
                for (int r = 1; r < n; r++) ok &= connects(chain[r - 1], chain[r]);
                if (ok) {
                    for (int r = 0; r < n; r++) {
                        board[r] = chain[r].clone();
                        if (!(r == 0 || r == n - 1) || allowEnds) {
                            // scramble mutable rows only
                            List<Integer> l = new ArrayList<>();
                            for (int v : board[r]) l.add(v);
                            Collections.shuffle(l, rnd);
                            for (int k = 0; k < 4; k++) board[r][k] = l.get(k);
                        }
                    }
                }
            }

            int[][] copy = new int[n][];
            for (int r = 0; r < n; r++) copy[r] = board[r].clone();
            DnaSolver.Solution sol = DnaSolver.solve(copy, allowEnds);
            for (int r = 0; r < n; r++) if (!Arrays.equals(copy[r], board[r])) throw new AssertionError("solver mutated input");

            if (n == 4) {
                int expected = brute(board, allowEnds);
                int got = sol == null ? -1 : sol.swapsNeeded();
                if (expected != got) throw new AssertionError("optimality mismatch iter=" + iter + " expected=" + expected + " got=" + got);
                checkedOptimal++;
            }

            if (sol == null) { unsolvable++; continue; }
            solved++;

            if (sol.swaps().size() != sol.swapsNeeded()) throw new AssertionError("swap count mismatch");
            int[][] b = new int[n][];
            for (int r = 0; r < n; r++) b[r] = board[r].clone();
            for (DnaSolver.Swap s : sol.swaps()) {
                if (!allowEnds && (s.row() == 0 || s.row() == n - 1)) throw new AssertionError("touched fixed end row");
                int t = b[s.row()][s.a()]; b[s.row()][s.a()] = b[s.row()][s.b()]; b[s.row()][s.b()] = t;
            }
            for (int r = 0; r + 1 < n; r++) if (!connects(b[r], b[r + 1])) throw new AssertionError("result not connected at row " + r + " iter " + iter);

            // Any single swap from the list must reduce the remaining cost by exactly one.
            DnaSolver.Swap first = sol.swaps().isEmpty() ? null : sol.swaps().get(0);
            if (first != null) {
                int[][] c = new int[n][];
                for (int r = 0; r < n; r++) c[r] = board[r].clone();
                int t = c[first.row()][first.a()]; c[first.row()][first.a()] = c[first.row()][first.b()]; c[first.row()][first.b()] = t;
                DnaSolver.Solution next = DnaSolver.solve(c, allowEnds);
                if (next == null || next.swapsNeeded() != sol.swapsNeeded() - 1)
                    throw new AssertionError("first swap did not make progress iter " + iter);
            }
        }
        System.out.println("OK solved=" + solved + " unsolvable=" + unsolvable + " optimalityChecked=" + checkedOptimal);
    }
}
