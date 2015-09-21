package account

class AcDailReport {

  //账户
  AcAccount account
  //账户号
  String accountNo
  //日期
  Date reportDate
  //上期余额
  Long preBalance
  //本期余额
  Long balance
  //本期发生金额
  Long currentAmount
  //账务平衡
  Boolean isBalancing

  static constraints = {
  }

  static mapping = {
    id generator: 'sequence', params: [sequence: 'seq_ac_dail_report']
  }
}
