package account;

import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.StoredProcedure;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Types;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: leizhen
 * Date: 11-3-18
 * Time: 下午2:27
 * 更新账户余额存储过程调用
 */
public class UpdateBalanceProcedure extends StoredProcedure {

    public UpdateBalanceProcedure(DataSource ds) {
        super(ds, "UPDATE_BAL");
        setFunction(false);
        declareParameter(new SqlParameter("accountNo", Types.VARCHAR));
        declareParameter(new SqlParameter("amount", Types.NUMERIC));
        declareParameter(new SqlOutParameter("balance", Types.NUMERIC));
        compile();
    }

    public Long executeProc(String accountNo, Long amount) {
        HashMap input = new HashMap();
        input.put("accountNo", accountNo);
        input.put("amount", amount);
        Map out = execute(input);
        if (!out.isEmpty()) {
//            Set<Map.Entry> entrySet = out.entrySet();
//            for (Iterator<Map.Entry> iterator = entrySet.iterator(); iterator.hasNext();) {
//                Map.Entry o =  iterator.next();
//                System.out.println("===== k: " + o.getKey() + " ,value: " + o.getValue());
//            }
            return ((BigDecimal)out.get("balance")).longValue();
        } else {
            return null;
        }
    }
}
