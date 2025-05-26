// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.utils;

import com.jxon.juscore.mjcore.models.Observation;
import com.jxon.juscore.mjcore.models.Rule;

import java.util.*;

public final class Search {

    private Search() {} // Prevent instantiation

    public static byte[][] run(byte[] present, int[] future, Rule[] rules, int MX, int MY, int MZ, int C,
                               boolean all, int limit, double depthCoefficient, int seed) {
        int[][] bpotentials = AH.array2D(C, present.length, -1);
        int[][] fpotentials = AH.array2D(C, present.length, -1);

        Observation.computeBackwardPotentials(bpotentials, future, MX, MY, MZ, rules);
        int rootBackwardEstimate = Observation.backwardPointwise(bpotentials, present);
        Observation.computeForwardPotentials(fpotentials, present, MX, MY, MZ, rules);
        int rootForwardEstimate = Observation.forwardPointwise(fpotentials, future);

        if (rootBackwardEstimate < 0 || rootForwardEstimate < 0) {
            System.out.println("INCORRECT PROBLEM");
            return null;
        }

        System.out.println("root estimate = (" + rootBackwardEstimate + ", " + rootForwardEstimate + ")");
        if (rootBackwardEstimate == 0) {
            return new byte[0][];
        }

        Board rootBoard = new Board(present.clone(), -1, 0, rootBackwardEstimate, rootForwardEstimate);

        List<Board> database = new ArrayList<>();
        database.add(rootBoard);
        Map<byte[], Integer> visited = new HashMap<>();
        visited.put(present, 0);

        PriorityQueue<FrontierItem> frontier = new PriorityQueue<>(Comparator.comparingDouble(item -> item.priority));
        Random random = new Random(seed);
        frontier.offer(new FrontierItem(0, rootBoard.rank(random, depthCoefficient)));
        int frontierLength = 1;

        int record = rootBackwardEstimate + rootForwardEstimate;
        while (frontierLength > 0 && (limit < 0 || database.size() < limit)) {
            int parentIndex = Objects.requireNonNull(frontier.poll()).index;
            frontierLength--;
            Board parentBoard = database.get(parentIndex);

            List<byte[]> children = all ? allChildStates(parentBoard.state, MX, MY, rules) :
                    oneChildStates(parentBoard.state, MX, MY, rules);

            for (byte[] childState : children) {
                Integer childIndex = visited.get(childState);
                if (childIndex != null) {
                    Board oldBoard = database.get(childIndex);
                    if (parentBoard.depth + 1 < oldBoard.depth) {
                        oldBoard.depth = parentBoard.depth + 1;
                        oldBoard.parentIndex = parentIndex;

                        if (oldBoard.backwardEstimate >= 0 && oldBoard.forwardEstimate >= 0) {
                            frontier.offer(new FrontierItem(childIndex, oldBoard.rank(random, depthCoefficient)));
                            frontierLength++;
                        }
                    }
                } else {
                    int childBackwardEstimate = Observation.backwardPointwise(bpotentials, childState);
                    Observation.computeForwardPotentials(fpotentials, childState, MX, MY, MZ, rules);
                    int childForwardEstimate = Observation.forwardPointwise(fpotentials, future);

                    if (childBackwardEstimate < 0 || childForwardEstimate < 0) {
                        continue;
                    }

                    Board childBoard = new Board(childState, parentIndex, parentBoard.depth + 1,
                            childBackwardEstimate, childForwardEstimate);
                    database.add(childBoard);
                    childIndex = database.size() - 1;
                    visited.put(childBoard.state, childIndex);

                    if (childBoard.forwardEstimate == 0) {
                        System.out.println("found a trajectory of length " + (parentBoard.depth + 1) +
                                ", visited " + visited.size() + " states");
                        List<Board> trajectory = Board.trajectory(childIndex, database);
                        Collections.reverse(trajectory);
                        return trajectory.stream().map(b -> b.state).toArray(byte[][]::new);
                    } else {
                        if (limit < 0 && childBackwardEstimate + childForwardEstimate <= record) {
                            record = childBackwardEstimate + childForwardEstimate;
                            System.out.println("found a state of record estimate " + record + " = " +
                                    childBackwardEstimate + " + " + childForwardEstimate);
                            print(childState, MX, MY);
                        }
                        frontier.offer(new FrontierItem(childIndex, childBoard.rank(random, depthCoefficient)));
                        frontierLength++;
                    }
                }
            }
        }

        return null;
    }

    private static List<byte[]> oneChildStates(byte[] state, int MX, int MY, Rule[] rules) {
        List<byte[]> result = new ArrayList<>();
        for (Rule rule : rules) {
            for (int y = 0; y < MY; y++) {
                for (int x = 0; x < MX; x++) {
                    if (matches(rule, x, y, state, MX, MY)) {
                        result.add(applied(rule, x, y, state, MX));
                    }
                }
            }
        }
        return result;
    }

    private static boolean matches(Rule rule, int x, int y, byte[] state, int MX, int MY) {
        if (x + rule.IMX > MX || y + rule.IMY > MY) {
            return false;
        }

        int dy = 0, dx = 0;
        for (int di = 0; di < rule.input.length; di++) {
            if ((rule.input[di] & (1 << state[x + dx + (y + dy) * MX])) == 0) {
                return false;
            }
            dx++;
            if (dx == rule.IMX) {
                dx = 0;
                dy++;
            }
        }
        return true;
    }

    private static byte[] applied(Rule rule, int x, int y, byte[] state, int MX) {
        byte[] result = state.clone();
        for (int dz = 0; dz < rule.OMZ; dz++) {
            for (int dy = 0; dy < rule.OMY; dy++) {
                for (int dx = 0; dx < rule.OMX; dx++) {
                    byte newValue = rule.output[dx + dy * rule.OMX + dz * rule.OMX * rule.OMY];
                    if (newValue != (byte) 0xff) {
                        result[x + dx + (y + dy) * MX] = newValue;
                    }
                }
            }
        }
        return result;
    }

    private static void print(byte[] state, int MX, int MY) {
        char[] characters = {'.', 'R', 'W', '#', 'a', '!', '?', '%', '0', '1', '2', '3', '4', '5'};
        for (int y = 0; y < MY; y++) {
            for (int x = 0; x < MX; x++) {
                System.out.print(characters[state[x + y * MX]] + " ");
            }
            System.out.println();
        }
    }

    public static boolean isInside(Rule.Tuple3 p, Rule rule, int x, int y) {
        return x <= p.x() && p.x() < x + rule.IMX && y <= p.y() && p.y() < y + rule.IMY;
    }

    public static boolean overlap(Rule rule0, int x0, int y0, Rule rule1, int x1, int y1) {
        for (int dy = 0; dy < rule0.IMY; dy++) {
            for (int dx = 0; dx < rule0.IMX; dx++) {
                if (isInside(new Rule.Tuple3(x0 + dx, y0 + dy, 0), rule1, x1, y1)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static List<byte[]> allChildStates(byte[] state, int MX, int MY, Rule[] rules) {
        List<RuleTileMatch> list = new ArrayList<>();
        int[] amounts = new int[state.length];

        for (int i = 0; i < state.length; i++) {
            int x = i % MX, y = i / MX;
            for (Rule rule : rules) {
                if (matches(rule, x, y, state, MX, MY)) {
                    list.add(new RuleTileMatch(rule, i));
                    for (int dy = 0; dy < rule.IMY; dy++) {
                        for (int dx = 0; dx < rule.IMX; dx++) {
                            amounts[x + dx + (y + dy) * MX]++;
                        }
                    }
                }
            }
        }

        RuleTileMatch[] tiles = list.toArray(new RuleTileMatch[0]);
        boolean[] mask = AH.array1D(tiles.length, true);
        List<RuleTileMatch> solution = new ArrayList<>();

        List<byte[]> result = new ArrayList<>();
        enumerate(result, solution, tiles, amounts, mask, state, MX);
        return result;
    }

    private static void enumerate(List<byte[]> children, List<RuleTileMatch> solution, RuleTileMatch[] tiles,
                                  int[] amounts, boolean[] mask, byte[] state, int MX) {
        int I = Helper.maxPositiveIndex(amounts);
        int X = I % MX, Y = I / MX;
        if (I < 0) {
            children.add(apply(state, solution, MX));
            return;
        }

        List<RuleTileMatch> cover = new ArrayList<>();
        for (int l = 0; l < tiles.length; l++) {
            RuleTileMatch tile = tiles[l];
            if (mask[l] && isInside(new Rule.Tuple3(X, Y, 0), tile.rule, tile.i % MX, tile.i / MX)) {
                cover.add(tile);
            }
        }

        for (RuleTileMatch tile : cover) {
            solution.add(tile);

            List<Integer> intersecting = new ArrayList<>();
            for (int l = 0; l < tiles.length; l++) {
                if (mask[l]) {
                    RuleTileMatch tile1 = tiles[l];
                    if (overlap(tile.rule, tile.i % MX, tile.i / MX, tile1.rule, tile1.i % MX, tile1.i / MX)) {
                        intersecting.add(l);
                    }
                }
            }

            for (int l : intersecting) {
                hide(l, false, tiles, amounts, mask, MX);
            }
            enumerate(children, solution, tiles, amounts, mask, state, MX);
            for (int l : intersecting) {
                hide(l, true, tiles, amounts, mask, MX);
            }

            solution.remove(solution.size() - 1);
        }
    }

    private static void hide(int l, boolean unhide, RuleTileMatch[] tiles, int[] amounts, boolean[] mask, int MX) {
        mask[l] = unhide;
        RuleTileMatch tile = tiles[l];
        int x = tile.i % MX, y = tile.i / MX;
        int incr = unhide ? 1 : -1;

        for (int dy = 0; dy < tile.rule.IMY; dy++) {
            for (int dx = 0; dx < tile.rule.IMX; dx++) {
                amounts[x + dx + (y + dy) * MX] += incr;
            }
        }
    }

    private static void apply(Rule rule, int x, int y, byte[] state, int MX) {
        for (int dy = 0; dy < rule.OMY; dy++) {
            for (int dx = 0; dx < rule.OMX; dx++) {
                state[x + dx + (y + dy) * MX] = rule.output[dx + dy * rule.OMX];
            }
        }
    }

    private static byte[] apply(byte[] state, List<RuleTileMatch> solution, int MX) {
        byte[] result = state.clone();
        for (RuleTileMatch tile : solution) {
            apply(tile.rule, tile.i % MX, tile.i / MX, result, MX);
        }
        return result;
    }

    private record RuleTileMatch(Rule rule, int i) {
    }

    private record FrontierItem(int index, double priority) {
    }
}

class Board {
    public byte[] state;
    public int parentIndex, depth, backwardEstimate, forwardEstimate;

    public Board(byte[] state, int parentIndex, int depth, int backwardEstimate, int forwardEstimate) {
        this.state = state;
        this.parentIndex = parentIndex;
        this.depth = depth;
        this.backwardEstimate = backwardEstimate;
        this.forwardEstimate = forwardEstimate;
    }

    public double rank(Random random, double depthCoefficient) {
        double result = depthCoefficient < 0.0 ? 1000 - depth :
                forwardEstimate + backwardEstimate + 2.0 * depthCoefficient * depth;
        return result + 0.0001 * random.nextDouble();
    }

    public static List<Board> trajectory(int index, List<Board> database) {
        List<Board> result = new ArrayList<>();
        for (Board board = database.get(index); board.parentIndex >= 0; board = database.get(board.parentIndex)) {
            result.add(board);
        }
        return result;
    }
}

