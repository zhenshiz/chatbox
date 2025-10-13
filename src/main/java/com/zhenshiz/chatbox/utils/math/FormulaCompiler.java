package com.zhenshiz.chatbox.utils.math;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

/**
 * 手写公式编译器：把含 x,y 的数学表达式编译成 BiFunction<Float,Float,Float>
 * 支持 + - * / ^ 以及 sin/cos/tan/log/exp/abs 和括号
 */
public class FormulaCompiler {

    /* ====================== 对外唯一入口 ====================== */
    public static BiFunction<Float, Float, Float> compile(String text) {
        Lexer lexer = new Lexer(text);
        List<Token> tokens = lexer.scan();
        Parser parser = new Parser(tokens);
        parser.parse();
        return new VmFunction(parser.code(), parser.consts());
    }

    /* ====================== 1. 词法 ====================== */
    private enum TkType {
        NUM, VAR, FUN, OP, LP, RP, EOF
    }

    private static class Token {
        TkType type;
        String text;
        double numVal;  // 仅 NUM 用
        Token(TkType t, String s) { this(t, s, 0); }
        Token(TkType t, String s, double v) { type = t; text = s; numVal = v; }
    }

    private static class Lexer {
        private final String src;
        private int pos = 0;
        Lexer(String src) { this.src = src.replaceAll("\\s+", ""); }
        List<Token> scan() {
            List<Token> tokens = new ArrayList<>();
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (Character.isDigit(c) || c == '.') {  // 数字
                    int st = pos;
                    while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) pos++;
                    double v = Double.parseDouble(src.substring(st, pos));
                    tokens.add(new Token(TkType.NUM, src.substring(st, pos), v));
                } else if (Character.isLetter(c)) {      // 变量、函数、或常数 pi/e
                    int st = pos;
                    while (pos < src.length() && Character.isLetter(src.charAt(pos))) pos++;
                    String word = src.substring(st, pos);
                    // 把 pi 和 e 直接当成数字
                    if (word.equals("pi")) {
                        tokens.add(new Token(TkType.NUM, "pi", Math.PI));
                    } else if (word.equals("e")) {
                        tokens.add(new Token(TkType.NUM, "e", Math.E));
                    } else if (pos < src.length() && src.charAt(pos) == '(') {  // 函数
                        tokens.add(new Token(TkType.FUN, word));
                    } else {  // 变量
                        tokens.add(new Token(TkType.VAR, word));
                    }
                } else if (c == '+' || c == '-' || c == '*' || c == '/' || c == '^') {
                    tokens.add(new Token(TkType.OP, "" + c)); pos++;
                } else if (c == '(') { tokens.add(new Token(TkType.LP, "(")); pos++;
                } else if (c == ')') { tokens.add(new Token(TkType.RP, ")")); pos++;
                } else throw new RuntimeException("非法字符: " + c);
            }
            tokens.add(new Token(TkType.EOF, ""));
            return tokens;
        }
    }

    /* ====================== 2. 语法分析 + 代码生成 ====================== */
    private static class Parser {
        private final List<Token> tokens;
        private int cur = 0;
        private final List<Integer> code = new ArrayList<>();
        private final List<Double> consts = new ArrayList<>();
        Parser(List<Token> tokens) { this.tokens = tokens; }
        List<Integer> code() { return code; }
        List<Double> consts() { return consts; }
        /* -------------------- 递归下降 -------------------- */
        void parse() { expr(); expect(TkType.EOF); }
        private void expr() {          // add/sub
            term();
            while (matchOp("+") || matchOp("-")) {
                String op = prevOp();
                term();
                emit(op.equals("+") ? Op.ADD : Op.SUB);
            }
        }
        private void term() {          // mul/div
            power();
            while (matchOp("*") || matchOp("/")) {
                String op = prevOp();
                power();
                emit(op.equals("*") ? Op.MUL : Op.DIV);
            }
        }
        private void power() {         // ^ 右结合
            unary();
            if (matchOp("^")) {
                power();               // 右递归
                emit(Op.POW);
            }
        }
        private void unary() {         // 一元负号
            if (matchOp("-")) {
                unary();
                emit(Op.NEG);
            } else primary();
        }
        private void primary() {
            if (match(TkType.NUM)) {
                double v = tokens.get(cur - 1).numVal;
                emit(Op.LOADC, addConst(v));
            } else if (match(TkType.VAR)) {
                String name = tokens.get(cur - 1).text;
                if (name.equals("x")) emit(Op.LOADX);
                else if (name.equals("y")) emit(Op.LOADY);
                else throw new RuntimeException("未知变量: " + name);
            } else if (match(TkType.FUN)) {
                String fun = tokens.get(cur - 1).text;
                expect(TkType.LP);
                expr();
                expect(TkType.RP);
                switch (fun) {
                    case "sin"  -> emit(Op.SIN);
                    case "cos"  -> emit(Op.COS);
                    case "tan"  -> emit(Op.TAN);
                    case "log"  -> emit(Op.LOG);
                    case "ln"   -> emit(Op.LN);
                    case "exp"  -> emit(Op.EXP);
                    case "abs"  -> emit(Op.ABS);
                    // 反三角
                    case "asin" -> emit(Op.ASIN);
                    case "acos" -> emit(Op.ACOS);
                    case "atan" -> emit(Op.ATAN);
                    // 双曲
                    case "sinh" -> emit(Op.SINH);
                    case "cosh" -> emit(Op.COSH);
                    case "tanh" -> emit(Op.TANH);
                    // 其它
                    case "sqrt" -> emit(Op.SQRT);
                    default -> throw new RuntimeException("未知函数: " + fun);
                }
            } else if (match(TkType.LP)) {
                expr();
                expect(TkType.RP);
            } else throw new RuntimeException("语法错误");
        }
        /* -------------------- 工具 -------------------- */
        private boolean match(TkType t) {
            if (cur < tokens.size() && tokens.get(cur).type == t) { cur++; return true; }
            return false;
        }
        private boolean matchOp(String op) {
            if (cur < tokens.size() && tokens.get(cur).type == TkType.OP && tokens.get(cur).text.equals(op)) {
                cur++; return true;
            }
            return false;
        }
        private String prevOp() { return tokens.get(cur - 1).text; }
        private void expect(TkType t) {
            if (!match(t)) throw new RuntimeException("期望 " + t + " 但看到 " + tokens.get(cur).text);
        }
        private int addConst(double v) { consts.add(v); return consts.size() - 1; }
        private void emit(int op) { code.add(op); }
        private void emit(int op, int arg) { code.add(op); code.add(arg); }
    }

    /* ====================== 3. 指令集 ====================== */
    private static class Op {
        static final int LOADC = 1; // 后接常量池下标
        static final int LOADX = 2;
        static final int LOADY = 3;
        static final int ADD   = 4;
        static final int SUB   = 5;
        static final int MUL   = 6;
        static final int DIV   = 7;
        static final int NEG   = 8;
        static final int POW   = 9;
        static final int SQRT  = 10;
        static final int SIN   = 11;
        static final int COS   = 12;
        static final int TAN   = 13;
        static final int LN    = 14;
        static final int LOG   = 15;
        static final int EXP   = 16;
        static final int ABS   = 17;
        static final int ASIN  = 18;
        static final int ACOS  = 19;
        static final int ATAN  = 20;
        static final int SINH  = 21;
        static final int COSH  = 22;
        static final int TANH  = 23;
    }

    /* ====================== 4. 虚拟机函数 ====================== */
    private static class VmFunction implements BiFunction<Float, Float, Float> {
        private final int[] code;
        private final double[] consts;
        VmFunction(List<Integer> c, List<Double> cons) {
            code = c.stream().mapToInt(Integer::intValue).toArray();
            consts = cons.stream().mapToDouble(Double::doubleValue).toArray();
        }
        @Override
        public Float apply(Float x, Float y) {
            float[] stack = new float[32];
            int sp = -1;
            for (int pc = 0; pc < code.length; ) {
                int op = code[pc++];
                switch (op) {
                    case Op.LOADC -> stack[++sp] = (float) consts[code[pc++]];
                    case Op.LOADX -> stack[++sp] = x;
                    case Op.LOADY -> stack[++sp] = y;
                    case Op.ADD -> { float b = stack[sp--], a = stack[sp--]; stack[++sp] = a + b; }
                    case Op.SUB -> { float b = stack[sp--], a = stack[sp--]; stack[++sp] = a - b; }
                    case Op.MUL -> { float b = stack[sp--], a = stack[sp--]; stack[++sp] = a * b; }
                    case Op.DIV -> { float b = stack[sp--], a = stack[sp--]; stack[++sp] = a / b; }
                    case Op.POW -> { float b = stack[sp--], a = stack[sp--]; stack[++sp] = (float) Math.pow(a, b); }
                    case Op.NEG -> stack[sp] = -stack[sp];
                    case Op.SIN -> stack[sp] = (float) Math.sin(stack[sp]);
                    case Op.COS -> stack[sp] = (float) Math.cos(stack[sp]);
                    case Op.TAN -> stack[sp] = (float) Math.tan(stack[sp]);
                    case Op.LN ->  stack[sp] = (float) Math.log(stack[sp]);
                    case Op.LOG -> stack[sp] = (float) Math.log10(stack[sp]);
                    case Op.EXP -> stack[sp] = (float) Math.exp(stack[sp]);
                    case Op.ABS -> stack[sp] = Math.abs(stack[sp]);
                    case Op.ASIN -> stack[sp] = (float) Math.asin(stack[sp]);
                    case Op.ACOS -> stack[sp] = (float) Math.acos(stack[sp]);
                    case Op.ATAN -> stack[sp] = (float) Math.atan(stack[sp]);
                    case Op.SINH -> stack[sp] = (float) Math.sinh(stack[sp]);
                    case Op.COSH -> stack[sp] = (float) Math.cosh(stack[sp]);
                    case Op.TANH -> stack[sp] = (float) Math.tanh(stack[sp]);
                    case Op.SQRT -> stack[sp] = (float) Math.sqrt(stack[sp]);
                    default -> throw new RuntimeException("非法指令");
                }
            }
            return stack[0];
        }
    }
}