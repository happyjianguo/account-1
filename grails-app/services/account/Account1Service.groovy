package account

import groovy.sql.Sql
import org.hibernate.LockMode

/**
 * unlock version
 */
class Account1Service {

  static transactional = false

  def sessionFactory
  def dataSource

  /**
   * 生成账号，16位，左边日期数字(2011年1月1日到现在的天数) + 0.... + 序号
   * @return 账号
   */
  def genAccNo() {
    def sql = new Sql(dataSource)
    def seq = sql.firstRow('select seq_ac_accno.NEXTVAL from DUAL')['NEXTVAL']
    def dates = new Date() - new Date(2010 - 1900, 0, 1)
    def padNum = 16 - dates.toString().size()
    def accNo = dates.toString() + seq.toString().padLeft(padNum, '0')
    //println "seq : ${seq}, dates : ${dates}, padNum : ${padNum}, accNo : ${accNo}"
    return accNo
  }

  /**
   * 生成账务流水号
   * @return
   */
  def genTransCode() {
    def sql = new Sql(dataSource)
    def seq = sql.firstRow('select seq_ac_transseq.NEXTVAL from DUAL')['NEXTVAL']
    return seq
  }

  /**
   * 冻结账户
   * @param accountNo 账号
   * @return
   * @throws Exception
   */
  def freezeAcc(accountNo) throws Exception {
    if (accountNo == null || accountNo == '') {
      throw new ErrorCodeException('01', '账号不能为空')
    }
    def mainAcc = AcAccount.findByAccountNo(accountNo)
    if (!mainAcc || mainAcc.accountType != 'main') {
      throw new ErrorCodeException('01', '账户不存在')
    }
    if (mainAcc.status == 'closed') {
      throw new ErrorCodeException('02', '账户已关闭')
    }

    def freezeAcc = AcAccount.findByParentIdAndAccountType(mainAcc.id, 'freeze')

    AcAccount.executeUpdate('update AcAccount a set a.status=\'freeze\' where a.id=?', [mainAcc.id])
    AcAccount.executeUpdate('update AcAccount a set a.status=\'freeze\' where a.id=?', [freezeAcc.id])
//    mainAcc.status = 'freeze'
//    freezeAcc.status = 'freeze'
//    mainAcc.save(failOnError: true)
//    freezeAcc.save(failOnError: true)
  }

  /**
   * 解冻账户
   * @param accountNo 账号
   * @return
   * @throws Exception
   */
  def unfreezeAcc(accountNo) throws Exception {
    if (accountNo == null || accountNo == '') {
      throw new ErrorCodeException('01', '账号不能为空')
    }

    def mainAcc = AcAccount.findByAccountNo(accountNo)
    if (!mainAcc || mainAcc.accountType != 'main') {
      throw new ErrorCodeException('01', '账户不存在')
    }
    if (mainAcc.status == 'closed') {
      throw new ErrorCodeException('02', '账户已关闭')
    }

    def freezeAcc = AcAccount.findByParentIdAndAccountType(mainAcc.id, 'freeze')

    AcAccount.executeUpdate('update AcAccount a set a.status=\'norm\' where a.id=?', [mainAcc.id])
    AcAccount.executeUpdate('update AcAccount a set a.status=\'norm\' where a.id=?', [freezeAcc.id])
//    mainAcc.status = 'norm'
//    freezeAcc.status = 'norm'
//    mainAcc.save(failOnError: true)
//    freezeAcc.save(failOnError: true)
  }

  /**
   * 关闭账户
   * @param accountNo 账号
   * @return
   * @throws Exception
   */
  def closeAcc(accountNo) throws Exception {
    if (accountNo == null || accountNo == '') {
      throw new ErrorCodeException('01', '账号不能为空')
    }

    def mainAcc = AcAccount.findByAccountNo(accountNo)
    if (!mainAcc || mainAcc.accountType != 'main') {
      throw new ErrorCodeException('01', '账户不存在')
    }
    if (mainAcc.status == 'closed') {
      throw new ErrorCodeException('02', '账户已关闭')
    }

    def freezeAcc = AcAccount.findByParentIdAndAccountType(mainAcc.id, 'freeze')

    AcAccount.executeUpdate('update AcAccount a set a.status=\'closed\' where a.id=?', [mainAcc.id])
    AcAccount.executeUpdate('update AcAccount a set a.status=\'closed\' where a.id=?', [freezeAcc.id])
//    mainAcc.status = 'closed'
//    freezeAcc.status = 'closed'
//    mainAcc.save(failOnError: true)
//    freezeAcc.save(failOnError: true)
  }

  /**
   * 执行指令集操作
   * @param commandSeqno 外部指令序号
   * @param commandLs 指令集合List<Map>
   * @return List < AcTransaction >  交易结果集合和
   * @throws Exception
   */
  def batchCommand(commandSeqno, commandLs) throws Exception {
    try {

      AcTransaction.withTransaction {
        if (!commandSeqno) {
          throw new ErrorCodeException('a2', '参数commandSeqno为空')
        }
        //查询重复指令集请求
        def oldTrans = AcTransaction.findByCommandSeqno(commandSeqno)
        if (oldTrans) {
          //原交易失败，返回交易结果集合
          if (oldTrans.resultCode != 00) {
            return [oldTrans]
          }
          //返回原指令结果
          return AcTransaction.findAllByCommandSeqno(commandSeqno, [sort: "transactionOrder", order: "asc"])
          //throw new ErrorCodeException('a1', '指令集序号重复')
        }
        if (commandLs == null || commandLs.size() == 0) {
          throw new ErrorCodeException('a2', '指令集为空')
        }
        //获取指令集账户，读取账户并按顺序给账户加锁
        def accSet = new TreeSet()
        commandLs.each {
          if (it.commandType == 'transfer') {
            accSet.add(it.fromAccountNo)
            accSet.add(it.toAccountNo)
          } else if (it.commandType == 'freeze' || it.commandType == 'unfreeze') {
            accSet.add(it.fromAccountNo)
          } else {
            throw new ErrorCodeException('a2', 'commandType错误')
          }
        }

        def accMap = [:]
        def session = sessionFactory.currentSession
        accSet.each {
          def acc = AcAccount.findByAccountNo(it)
          if (!acc) {
            throw new ErrorCodeException('01', "账户\"${it}\"不存在")
          }
          //session.evict(acc) // Force the following load to actually go to the DB instead of doing a version check
          //acc = session.load(AcAccount.class, acc.id, LockMode.UPGRADE)
          //acc.lock()
          accMap.put(it, acc)
        }

        def transactionCode = genTransCode()
        def transactionLs = []
        for (int i = 0; i < commandLs.size(); i++) {
          def commandParam = commandLs.get(i);
          if (commandParam.commandType == 'transfer') {
            //判断参数
            if (!commandParam.amount || !commandParam.amount.class == Long || !commandParam.transferType || !commandParam.tradeNo || !commandParam.outTradeNo) {
              throw new ErrorCodeException('a2', '参数错误')
            }
            if (commandParam.amount <= 0) {
              throw new ErrorCodeException('a2', 'amount必须大于0')
            }

            def transaction = new AcTransaction()
            transactionLs.add(transaction)
            transaction.transactionCode = transactionCode
            transaction.commandSeqno = commandSeqno
            transaction.commandType = commandParam.commandType
            transaction.amount = commandParam.amount.toLong()
            transaction.transferType = commandParam.transferType
            transaction.tradeNo = commandParam.tradeNo
            transaction.outTradeNo = commandParam.outTradeNo
            transaction.transactionOrder = i
            transaction.subjict = commandParam.subjict

            //执行交易
            commandTransfer(accMap[commandParam.fromAccountNo], accMap[commandParam.toAccountNo], transaction.amount, transaction)
          } else if (commandParam.commandType == 'freeze' || commandParam.commandType == 'unfreeze') {
            //判断参数
            if (!commandParam.amount || !commandParam.amount.class == Long || !commandParam.transferType || !commandParam.tradeNo || !commandParam.outTradeNo) {
              throw new ErrorCodeException('a2', '参数错误')
            }
            if (commandParam.amount <= 0) {
              throw new ErrorCodeException('a2', 'amount必须大于0')
            }

            def transaction = new AcTransaction()
            transactionLs.add(transaction)
            transaction.transactionCode = transactionCode
            transaction.commandSeqno = commandSeqno
            transaction.commandType = commandParam.commandType
            transaction.amount = commandParam.amount.toLong()
            transaction.transferType = commandParam.transferType
            transaction.tradeNo = commandParam.tradeNo
            transaction.outTradeNo = commandParam.outTradeNo
            transaction.transactionOrder = i
            transaction.subjict = commandParam.subjict

            //执行交易
            if (commandParam.commandType == 'freeze') {
              commandFreeze(accMap[commandParam.fromAccountNo], transaction.amount, transaction)
            } else {
              commandUnfreeze(accMap[commandParam.fromAccountNo], transaction.amount, transaction)
            }
          }
        }
        return transactionLs
      }
    } catch (ErrorCodeException e) {
      def transaction = new AcTransaction()
      transaction.transactionCode = genTransCode()
      transaction.commandSeqno = commandSeqno
      transaction.resultCode = e.getCode()
      transaction.commandType = 'error'
      transaction.transactionOrder = 0
      transaction.save()
      throw e
    } catch (Exception e) {
      def transaction = new AcTransaction()
      transaction.transactionCode = genTransCode()
      transaction.commandSeqno = commandSeqno
      transaction.resultCode = 'ff'
      transaction.commandType = 'error'
      transaction.transactionOrder = 0
      transaction.save()
      throw e
    }
  }

  /**
   * 转账指令操作
   * @param fromAcc 转出账户
   * @param toAcc 转入账户
   * @param amount 转账金额
   * @param transaction 指令事务
   * @return
   * @throws Exception
   */
  def commandTransfer(fromAcc, toAcc, amount, transaction) throws Exception {
    if (fromAcc == null || fromAcc.accountType != 'main') {
      throw new ErrorCodeException('01', '转出账户不存在')
    }
    if (toAcc == null || toAcc.accountType != 'main') {
      throw new ErrorCodeException('01', '转入账户不存在')
    }
    if (fromAcc.status != 'norm') {
      throw new ErrorCodeException('02', '转出账户状态不正常')
    }
    if (toAcc.status != 'norm') {
      throw new ErrorCodeException('02', '转入账户状态不正常')
    }

    //转出账户产生借记交易
    def debitSequential = new AcSequential()
    debitSequential.account = fromAcc
    debitSequential.accountNo = fromAcc.accountNo
    debitSequential.transaction = transaction
    debitSequential.debitAmount = amount

    //判断转出账户是借记还是贷记账户，借记账户减钱，贷记账户加钱
    def amount2Bal
    if (fromAcc.balanceOfDirection == 'debit') {
      //减钱
      amount2Bal = -amount
    } else {
      //加钱
      amount2Bal = amount
    }
    //更新账户余额并设置流水余额
    def proc = new UpdateBalanceProcedure(dataSource)  //生成可执行的存储过程
    int balance = proc.executeProc(fromAcc.accountNo, amount2Bal);//执行存储过程，得到余额。
    if (balance == null) {
      throw new ErrorCodeException('01', '转出账户不存在')
    }
    if (balance < 0) {
      throw new ErrorCodeException('03', '转出账户余额不足')
    }
    debitSequential.balance = balance
    debitSequential.preBalance = balance - amount2Bal

    //转入账户产生贷记交易
    def creditSequential = new AcSequential()
    creditSequential.account = toAcc
    creditSequential.accountNo = toAcc.accountNo
    creditSequential.transaction = transaction
    creditSequential.creditAmount = amount
    //判断转入账户是借记还是贷记账户，借记账户加钱，贷记账户减钱
    if (toAcc.balanceOfDirection == 'debit') {
      //加钱
      amount2Bal = amount
    } else {
      //减钱
      amount2Bal = -amount
    }
    //更新账户余额并设置流水余额
    balance = proc.executeProc(toAcc.accountNo, amount2Bal);
    if (balance == null) {
      throw new ErrorCodeException('01', '转入账户不存在')
    }
    if (balance < 0) {
      throw new ErrorCodeException('03', '转入账户余额不足')
    }
    creditSequential.balance = balance
    creditSequential.preBalance = balance - amount2Bal

    //在事务记录中记录账户信息
    transaction.fromAccount = fromAcc
    transaction.fromAccountNo = fromAcc.accountNo
    transaction.toAccount = toAcc
    transaction.toAccountNo = toAcc.accountNo

    transaction.save(failOnError: true)
    debitSequential.save(failOnError: true)
    creditSequential.save(failOnError: true)
  }

  /**
   * 冻结指令操作
   * @param account 冻结账户
   * @param amount 冻结金额
   * @param transaction 指令事务
   * @return
   * @throws Exception
   */
  def commandFreeze(account, amount, transaction) throws Exception {
    if (account == null || account.accountType != 'main') {
      throw new ErrorCodeException('01', '账户不存在')
    }
    if (account.status != 'norm') {
      throw new ErrorCodeException('02', '账户状态不正常')
    }

    def freezeAcc = AcAccount.findByParentIdAndAccountType(account.id, 'freeze')
    if (!freezeAcc) {
      throw new ErrorCodeException('04', '账户不支持冻结')
    }

    def sql = new Sql(dataSource)

    //转出账户产生借记交易
    def debitSequential = new AcSequential()
    debitSequential.account = account
    debitSequential.accountNo = account.accountNo
    debitSequential.transaction = transaction
    debitSequential.debitAmount = amount

    //判断转出账户是借记还是贷记账户，借记账户减钱，贷记账户加钱
    def amount2Bal
    if (account.balanceOfDirection == 'debit') {
      //减钱
      amount2Bal = -amount
    } else {
      //加钱
      amount2Bal = amount
    }
    //更新账户余额并设置流水余额

    def proc = new UpdateBalanceProcedure(dataSource)
    def balance = proc.executeProc(account.accountNo, amount2Bal);
    if (balance == null) {
      throw new ErrorCodeException('01', '冻结账户不存在')
    }
    if (balance < 0) {
      throw new ErrorCodeException('03', '冻结账户余额不足')
    }
    debitSequential.balance = balance
    debitSequential.preBalance = balance - amount2Bal

    //转入账户产生贷记交易
    def creditSequential = new AcSequential()
    creditSequential.account = freezeAcc
    creditSequential.accountNo = freezeAcc.accountNo
    creditSequential.transaction = transaction
    creditSequential.creditAmount = amount

    //判断转入账户是借记还是贷记账户，借记账户加钱，贷记账户减钱
    if (freezeAcc.balanceOfDirection == 'debit') {
      //减钱，判断余额
      amount2Bal = amount
    } else {
      //加钱
      amount2Bal = -amount
    }
    //更新账户余额并设置流水余额
    balance = proc.executeProc(freezeAcc.accountNo, amount2Bal);
    if (balance == null) {
      throw new ErrorCodeException('01', '冻结账户不存在')
    }
    if (balance < 0) {
      throw new ErrorCodeException('03', '冻结账户余额不足')
    }
    creditSequential.balance = balance
    creditSequential.preBalance = balance - amount2Bal

    //在事务记录中记录账户信息
    transaction.fromAccount = account
    transaction.fromAccountNo = account.accountNo
    transaction.toAccount = freezeAcc
    transaction.toAccountNo = freezeAcc.accountNo

    transaction.save(failOnError: true)
    debitSequential.save(failOnError: true)
    creditSequential.save(failOnError: true)
  }

  /**
   * 解冻指令操作
   * @param account 解冻账户
   * @param amount 解冻金额
   * @param transaction 解冻指令事务
   * @return
   * @throws Exception
   */
  def commandUnfreeze(account, amount, transaction) throws Exception {
    if (account == null || account.accountType != 'main') {
      throw new ErrorCodeException('01', '账户不存在')
    }
    if (account.status != 'norm') {
      throw new ErrorCodeException('02', '账户状态不正常')
    }

    def freezeAcc = AcAccount.findByParentIdAndAccountType(account.id, 'freeze')
    if (!freezeAcc) {
      throw new ErrorCodeException('04', '账户不支持冻结')
    }

    def sql = new Sql(dataSource)

    //转出账户产生借记交易
    def debitSequential = new AcSequential()
    debitSequential.account = freezeAcc
    debitSequential.accountNo = freezeAcc.accountNo
    debitSequential.transaction = transaction
    debitSequential.debitAmount = amount

    //判断转出账户是借记还是贷记账户，借记账户减钱，贷记账户加钱
    def amount2Bal
    if (freezeAcc.balanceOfDirection == 'debit') {
      //减钱
      amount2Bal = -amount
    } else {
      //加钱
      amount2Bal = amount
    }
    //更新账户余额并设置流水余额
    def proc = new UpdateBalanceProcedure(dataSource)
    def balance = proc.executeProc(freezeAcc.accountNo, amount2Bal);
    if (balance == null) {
      throw new ErrorCodeException('01', '冻结账户不存在' + freezeAcc.accountNo)
    }
    if (balance < 0) {
      throw new ErrorCodeException('03', '冻结账户余额不足')
    }
    debitSequential.balance = balance
    debitSequential.preBalance = balance - amount2Bal

    //转入账户产生贷记交易
    def creditSequential = new AcSequential()
    creditSequential.account = account
    creditSequential.accountNo = account.accountNo
    creditSequential.transaction = transaction
    creditSequential.creditAmount = amount
    //判断转入账户是借记还是贷记账户，借记账户加钱，贷记账户减钱
    if (account.balanceOfDirection == 'debit') {
      //减钱
      amount2Bal = amount
    } else {
      //加钱
      amount2Bal = -amount
    }
    //更新账户余额并设置流水余额
    balance = proc.executeProc(account.accountNo, amount2Bal);
    if (balance == null) {
      throw new ErrorCodeException('01', '冻结账户不存在')
    }
    if (balance < 0) {
      throw new ErrorCodeException('03', '冻结账户余额不足')
    }
    creditSequential.balance = balance
    creditSequential.preBalance = balance - amount2Bal

    //在事务记录中记录账户信息
    transaction.fromAccount = account
    transaction.fromAccountNo = account.accountNo
    transaction.toAccount = freezeAcc
    transaction.toAccountNo = freezeAcc.accountNo

    transaction.save(failOnError: true)
    debitSequential.save(failOnError: true)
    creditSequential.save(failOnError: true)
  }
}
