package account;

import org.springframework.jdbc.object.StoredProcedure;

import javax.sql.DataSource;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlInOutParameter;
import org.springframework.jdbc.object.*;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: zhouyong-pc
 * Date: 15-8-20
 * Time: 上午11:20
 * To change this template use File | Settings | File Templates.
 */
public class StoredProcedureTemple extends StoredProcedure {
    private static final String PROCEDURE_NAME = "CountTable";
    public static final String IN_PARAMETER_NAME = "tableName";
    public static final String OUT_PARAMETER_NAME = "sqlStr";
    public static final String INOUT_PARAMETER_NAME = "v";
    public StoredProcedureTemple(DataSource dataSource) {
        super(dataSource,PROCEDURE_NAME);
         // setFunction(true);
         declareParameter(new SqlParameter(IN_PARAMETER_NAME, Types.VARCHAR));
         declareParameter(new SqlOutParameter(OUT_PARAMETER_NAME,Types.VARCHAR));
         declareParameter(new SqlInOutParameter(INOUT_PARAMETER_NAME,Types.INTEGER));
         compile();
    }
    public Long doCountTable(String tableName,Integer v) {
        Map paraMap = new HashMap();
        paraMap.put(IN_PARAMETER_NAME, tableName);
        paraMap.put(INOUT_PARAMETER_NAME, v);
        Map resultMap = execute(paraMap);
        Object result = new Object();
//        result.setSql((String)resultMap.get(OUT_PARAMETER_NAME));
//        result.setCount((Integer)resultMap.get(INOUT_PARAMETER_NAME));
        return Long.decode("1");
    }
}
