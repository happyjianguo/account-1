package account

import grails.converters.JSON

class RpcController {

  def accountService

  /**
   * 开户调用，需要参数：{accountName: 'name', direction: 'debit or credit'}* accountName 账户名,必须参数；
   * direction 账户方向，debit 为借记账户，credit 为贷记账户,必须参数
   * 返回 : {result: 'true or false',errorCode:'', errorMsg: '', accountNo: ''}* result: true为成功， false 为失败,
   * errorCode: 当result为false时，返回误号 
   * errorMsg: 当result为false时，返回误原因,
   * accountNo: 当result为true时，返回账户账号
   */
  def openAcc = {
    try {
      def param = request.JSON
      def accountName = param.accountName
      def direction = param.direction
      if (accountName == null || accountName == '') {
        throw new ErrorCodeException('a2', '账户名不能为空')
      }
      if (direction == null || !['debit', 'credit'].contains(direction)) {
        throw new ErrorCodeException('a2', 'direction 参数错误')
      }
      def mainAcc = new AcAccount()
      mainAcc.accountNo = accountService.genAccNo()
      mainAcc.accountName = accountName
      mainAcc.balanceOfDirection = direction
      mainAcc.accountType = 'main'
      mainAcc.overdraft = param.overdraft
      mainAcc.save(failOnError: true)
      def freezeAcc = new AcAccount()
      freezeAcc.accountNo = accountService.genAccNo()
      freezeAcc.accountName = accountName
      freezeAcc.balanceOfDirection = 'debit'
      freezeAcc.accountType = 'freeze'
      freezeAcc.parentId = mainAcc.id
      freezeAcc.overdraft = false
      freezeAcc.save(failOnError: true)

      def result = [result: 'true', accountNo: mainAcc.accountNo]
      render result as JSON
    } catch (ErrorCodeException e) {
      log.warn('open account faile', e)
      def result = [result: 'false', errorCode: e.getCode(), errorMsg: e.getMessage()]
      render result as JSON
      return
    } catch (Exception e) {
      log.warn('open account faile', e)
      def result = [result: 'false', errorCode: 'ff', errorMsg: e.getMessage()]
      render result as JSON
      return
    }
  }

  /**
   * 冻结账户调用，需要参数：{accountNo: 'accNo'} accountNo 账户账号,必须参数；
   * 返回 : {result: 'true or false', errorCode: '', errorMsg: ''}* result: true为成功， false 为失败,
   * errorCode: 当result为false时，返回误号
   * errorMsg: 当result为false时，返回误原因,
   */
  def freezeAcc = {
    def param = request.JSON
    def accountNo = param.accountNo
    try {
      accountService.freezeAcc(accountNo)
      def result = [result: 'true']
      render result as JSON
      return
    } catch (ErrorCodeException e) {
      log.warn('freeze account faile', e)
      def result = [result: 'false', errorCode: e.getCode(), errorMsg: e.getMessage()]
      render result as JSON
      return
    } catch (Exception e) {
      log.warn('freeze account faile', e)
      def result = [result: 'false', errorCode: 'ff', errorMsg: e.getMessage()]
      render result as JSON
      return
    }
  }

  /**
   * 解冻冻账户调用，需要参数：{accountNo: 'accNo'} accountNo 账户账号,必须参数；
   * 返回 : {result: 'true or false', errorCode: '', errorMsg: ''}* result: true为成功， false 为失败,
   * errorCode: 当result为false时，返回误号
   * errorMsg: 当result为false时，返回误原因,
   */
  def unfreezeAcc = {
    def param = request.JSON
    def accountNo = param.accountNo
    try {
      accountService.unfreezeAcc(accountNo)
      def result = [result: 'true']
      render result as JSON
      return
    } catch (ErrorCodeException e) {
      log.warn('unfreeze account faile', e)
      def result = [result: 'false', errorCode: e.getCode(), errorMsg: e.getMessage()]
      render result as JSON
      return
    } catch (Exception e) {
      log.warn('unfreeze account faile', e)
      def result = [result: 'false', errorCode: 'ff', errorMsg: e.getMessage()]
      render result as JSON
      return
    }
  }

  /**
   * 关闭账户调用，需要参数：{accountNo: 'accNo'} accountNo 账户账号,必须参数；
   * 返回 : {result: 'true or false', errorCode:'', errorMsg: ''}* result: true为成功， false 为失败,
   * errorCode: 当result为false时，返回误号
   * errorMsg: 当result为false时，返回误原因,
   */
  def closeAcc = {
    def param = request.JSON
    def accountNo = param.accountNo
    try {
      accountService.closeAcc(accountNo)
      def result = [result: 'true']
      render result as JSON
      return
    } catch (ErrorCodeException e) {
      log.warn('close account faile', e)
      def result = [result: 'false', errorCode: e.getCode(), errorMsg: e.getMessage()]
      render result as JSON
      return
    } catch (Exception e) {
      log.warn('close account faile', e)
      def result = [result: 'false', errorCode: 'ff', errorMsg: e.getMessage()]
      render result as JSON
      return
    }
  }

  /**
   * 交易指令集调用，需要参数:
   *{* commandSeqno: '', //commandSeqno 账户账号,必须参数；
   * commandLs:[
   *{commandType:'transfer', fromAccountNo:'', toAccountNo:'', amount:'', transferType:'', tradeNo:'', outTradeNo:'', notes:''},
   *{commandType:'freeze', fromAccountNo:'', amount:'', transferType:'', tradeNo:'', outTradeNo:'', notes:''},
   *{commandType:'unfreeze', fromAccountNo:'', amount:'', transferType:'', tradeNo:'', outTradeNo:'', notes:''},
   * ]
   *}* 返回 : {result: 'true or false', transCode:'', transIds:['id1', 'id2',...] errorCode:'', errorMsg: ''}* result: true为成功， false 为失败,
   * transCode: 账务事务凭证号
   * transIds: 账户指令id集合
   * errorCode: 当result为false时，返回误号
   * errorMsg: 当result为false时，返回误原因,
   */
  def batchCommand = {

    def param = request.JSON
    try {
      def transList = accountService.batchCommand(param.commandSeqno, param.commandLs)
      def transIds = []
      transList.each {
        transIds.add(it.id)
      }
      def result
      if (transList && transList.size() > 0) {
        result = [result: 'true', transCode: transList.get(0).transactionCode, transIds: transIds, errorCode: '00']
        if (transList.get(0).commandType == 'error') {
          result = [result: 'false', errorCode: transList.get(0).resultCode, errorMsg: '失败的原交易']
        }
      } else {
        result = [result: 'true', transCode: '0', transIds: transIds, errorCode: '00']
      }
      render result as JSON
      return
    } catch (ErrorCodeException e) {
      log.warn('batchCommand faile', e)
      def result = [result: 'false', errorCode: e.getCode(), errorMsg: e.getMessage()]
      render result as JSON
      return
    } catch (Exception e) {
      log.warn('batchCommand faile', e)
      def result = [result: 'false', errorCode: 'ff', errorMsg: e.getMessage()]
      render result as JSON
      return
    }
  }

  /**
   * 查询账户信息，需要参数：{accountNo: 'accNo'} accountNo 账户账号,必须参数；
   * 返回 : {result: 'true or false', errorCode: '', errorMsg: '', accName: '', balance: '', freezBal:'', direc:'debit or credit', status:'norm or freeze or closed'}* result: true为成功， false 为失败,
   * errorCode: 当result为false时，返回误号
   * errorMsg: 当result为false时，返回误原因,
   */
  def queryAcc = {

    def param = request.JSON
    def accountNo = param.accountNo
    try {
      def account = AcAccount.findByAccountNo(accountNo)
      def freezAccount = AcAccount.findByParentId(account.id)
      if (!account) {
         throw new ErrorCodeException('01', '帐户不存在')
      }
      def result = [result: 'true', accName: account.accountName, balance: account.balance, freezBal: freezAccount.balance, direc: account.balanceOfDirection, status: account.status]
      render result as JSON
      return
    } catch (ErrorCodeException e) {
      log.warn('query account faile', e)
      def result = [result: 'false', errorCode: e.getCode(), errorMsg: e.getMessage()]
      render result as JSON
      return
    } catch (Exception e) {
      log.warn('query account faile', e)
      def result = [result: 'false', errorCode: 'ff', errorMsg: e.getMessage()]
      render result as JSON
      return
    }
  }
}
