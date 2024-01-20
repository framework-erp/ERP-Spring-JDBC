package erp.springjdbc.mysql;

public class MySQLUtil {
    public static String getMySQLDataType(String fieldType) {
        switch (fieldType) {
            case "byte":
                return "TINYINT";
            case "short":
                return "SMALLINT";
            case "char":
                return "SMALLINT";
            case "int":
                return "INT";
            case "long":
                return "BIGINT";
            case "float":
                return "FLOAT";
            case "double":
                return "DOUBLE";
            case "String":
                return "VARCHAR(128)";//TODO: 提供修改VARCHAR长度机制
            case "boolean":
                return "TINYINT(1)";
            default:
                return "VARCHAR(512)";//TODO: 提供修改VARCHAR长度机制
        }
    }
}
