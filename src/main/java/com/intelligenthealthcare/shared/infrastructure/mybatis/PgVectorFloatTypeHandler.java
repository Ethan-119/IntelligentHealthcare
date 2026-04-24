package com.intelligenthealthcare.shared.infrastructure.mybatis;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

/**
 * PostgreSQL {@code vector(n)} 与 {@code float[]} 映射，用于 RAG 嵌入列。
 */
@MappedTypes(float[].class)
@MappedJdbcTypes(JdbcType.OTHER)
public class PgVectorFloatTypeHandler extends BaseTypeHandler<float[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, float[] parameters, JdbcType jdbcType)
            throws SQLException {
        var g = new PGobject();
        g.setType("vector");
        g.setValue(toVectorLiteral(parameters));
        ps.setObject(i, g);
    }

    @Override
    public float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseVector(rs.getObject(columnName));
    }

    @Override
    public float[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseVector(rs.getObject(columnIndex));
    }

    @Override
    public float[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseVector(cs.getObject(columnIndex));
    }

    private static float[] parseVector(Object o) throws SQLException {
        if (o == null) {
            return null;
        }
        if (o instanceof PGobject pg) {
            if (pg.getValue() == null) {
                return null;
            }
            return fromLiteral(pg.getValue());
        }
        if (o instanceof String s) {
            return fromLiteral(s);
        }
        return null;
    }

    private static String toVectorLiteral(float[] v) {
        if (v == null || v.length == 0) {
            throw new IllegalArgumentException("vector 不可为空");
        }
        var sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }

    private static float[] fromLiteral(String s) {
        if (s == null || s.length() < 2) {
            return null;
        }
        var inner = s.charAt(0) == '[' ? s.substring(1, s.length() - 1) : s;
        if (inner.isEmpty()) {
            return new float[0];
        }
        var parts = inner.split(",");
        var a = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            a[i] = Float.parseFloat(parts[i].trim());
        }
        return a;
    }
}
