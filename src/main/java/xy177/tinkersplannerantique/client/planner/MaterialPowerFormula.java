package xy177.tinkersplannerantique.client.planner;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class MaterialPowerFormula {

    private MaterialPowerFormula() {
    }

    static double evaluate(String expression, Map<String, Double> variables) {
        if (expression == null || expression.trim().isEmpty()) {
            return 0D;
        }
        Parser parser = new Parser(expression, variables);
        double value = parser.parseExpression();
        parser.skipWhitespace();
        return parser.isDone() ? sanitize(value) : 0D;
    }

    private static double sanitize(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? 0D : value;
    }

    private static final class Parser {
        private final String expression;
        private final Map<String, Double> variables;
        private int index;

        Parser(String expression, Map<String, Double> variables) {
            this.expression = expression;
            this.variables = variables;
        }

        boolean isDone() {
            return index >= expression.length();
        }

        double parseExpression() {
            double value = parseTerm();
            while (true) {
                skipWhitespace();
                if (match('+')) {
                    value += parseTerm();
                } else if (match('-')) {
                    value -= parseTerm();
                } else {
                    return value;
                }
            }
        }

        private double parseTerm() {
            double value = parsePower();
            while (true) {
                skipWhitespace();
                if (peek("**")) {
                    return value;
                }
                if (match('*')) {
                    value *= parsePower();
                } else if (match('/')) {
                    double divisor = parsePower();
                    value = divisor == 0D ? 0D : value / divisor;
                } else {
                    return value;
                }
            }
        }

        private double parsePower() {
            double value = parseFactor();
            skipWhitespace();
            if (peek("**")) {
                index += 2;
                value = Math.pow(value, parsePower());
            }
            return value;
        }

        private double parseFactor() {
            skipWhitespace();
            if (match('+')) {
                return parseFactor();
            }
            if (match('-')) {
                return -parseFactor();
            }
            if (match('(')) {
                double value = parseExpression();
                match(')');
                return value;
            }
            if (index + 1 < expression.length() && expression.charAt(index) == '*' && expression.charAt(index + 1) == '.') {
                String name = parseName();
                Double value = variables.get(name.toLowerCase(Locale.ROOT));
                return value == null ? 0D : value.doubleValue();
            }
            if (peekNumber()) {
                return parseNumber();
            }
            String name = parseName();
            if (name.isEmpty()) {
                return 0D;
            }
            skipWhitespace();
            if (match('(')) {
                List<Double> args = new ArrayList<>();
                skipWhitespace();
                if (!match(')')) {
                    do {
                        args.add(Double.valueOf(parseExpression()));
                        skipWhitespace();
                    } while (match(','));
                    match(')');
                }
                return call(name, args);
            }
            Double value = variables.get(name.toLowerCase(Locale.ROOT));
            return value == null ? 0D : value.doubleValue();
        }

        private double parseNumber() {
            int start = index;
            while (index < expression.length()) {
                char c = expression.charAt(index);
                if ((c >= '0' && c <= '9') || c == '.') {
                    index++;
                } else {
                    break;
                }
            }
            try {
                return Double.parseDouble(expression.substring(start, index));
            } catch (NumberFormatException ignored) {
                return 0D;
            }
        }

        private String parseName() {
            int start = index;
            while (index < expression.length()) {
                char c = expression.charAt(index);
                if (Character.isLetterOrDigit(c) || c == '_' || c == '.' || c == '*') {
                    index++;
                } else {
                    break;
                }
            }
            return expression.substring(start, index).toLowerCase(Locale.ROOT);
        }

        private boolean peekNumber() {
            return index < expression.length() && ((expression.charAt(index) >= '0' && expression.charAt(index) <= '9') || expression.charAt(index) == '.');
        }

        private boolean match(char expected) {
            skipWhitespace();
            if (index < expression.length() && expression.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private boolean peek(String expected) {
            skipWhitespace();
            return expression.regionMatches(index, expected, 0, expected.length());
        }

        private void skipWhitespace() {
            while (index < expression.length() && Character.isWhitespace(expression.charAt(index))) {
                index++;
            }
        }

        private double call(String rawName, List<Double> args) {
            String name = rawName.toLowerCase(Locale.ROOT);
            if ("min".equals(name) && !args.isEmpty()) {
                double value = args.get(0).doubleValue();
                for (Double arg : args) {
                    value = Math.min(value, arg.doubleValue());
                }
                return value;
            }
            if ("max".equals(name) && !args.isEmpty()) {
                double value = args.get(0).doubleValue();
                for (Double arg : args) {
                    value = Math.max(value, arg.doubleValue());
                }
                return value;
            }
            if ("avg".equals(name) && !args.isEmpty()) {
                double sum = 0D;
                for (Double arg : args) {
                    sum += arg.doubleValue();
                }
                return sum / args.size();
            }
            if ("sqrt".equals(name) && args.size() == 1) {
                return Math.sqrt(Math.max(0D, args.get(0).doubleValue()));
            }
            if ("log".equals(name) && args.size() == 1) {
                double value = args.get(0).doubleValue();
                return value <= 0D ? 0D : Math.log(value);
            }
            if ("floor".equals(name) && args.size() == 1) {
                return Math.floor(args.get(0).doubleValue());
            }
            if ("ceil".equals(name) && args.size() == 1) {
                return Math.ceil(args.get(0).doubleValue());
            }
            if ("round".equals(name) && args.size() == 1) {
                return Math.round(args.get(0).doubleValue());
            }
            if ("abs".equals(name) && args.size() == 1) {
                return Math.abs(args.get(0).doubleValue());
            }
            if ("sign".equals(name) && args.size() == 1) {
                return Math.signum(args.get(0).doubleValue());
            }
            if ("clamp".equals(name) && args.size() == 3) {
                return Math.max(args.get(1).doubleValue(), Math.min(args.get(2).doubleValue(), args.get(0).doubleValue()));
            }
            return 0D;
        }
    }
}
