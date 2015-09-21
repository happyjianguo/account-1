package account

import grails.test.*
import grails.converters.JSON

class ClientServiceTests extends GroovyTestCase {

  def accountClientService

  protected void setUp() {
    super.setUp()
  }

  protected void tearDown() {
    super.tearDown()
  }

  void testOpenAcc() {  
    def res = accountClientService.openAcc('acc1', 'debit')
    assertEquals (res.result, 'true')
    res = accountClientService.openAcc('acc2', 'credit')
    assertEquals (res.result, 'true')
    res = accountClientService.openAcc('acc3', 'credit')
    assertEquals (res.result, 'true')
  }

  void testFreezeAcc() {
    def res = accountClientService.openAcc('acc1', 'debit')
    def acc1 = res.accountNo
    res = accountClientService.freezeAcc(acc1)
    assertEquals (res.result, 'true')
  }

  void testUnfreezeAcc() {
    def res = accountClientService.openAcc('acc1', 'debit')
    def acc1 = res.accountNo
    res = accountClientService.unfreezeAcc(acc1)
    assertEquals (res.result, 'true')
  }

  void testCloseAcc() {
    def res = accountClientService.openAcc('acc1', 'debit')
    def acc1 = res.accountNo
    res = accountClientService.closeAcc(acc1)
    assertEquals (res.result, 'true')
  }

  void testTransfer() {
    def commandLs = accountClientService.buildTransfer(null, '4560000000000004', '4560000000000002', 1000, 'trans', '1', '1', 'aaaaaaaaaaaa')
    def res = accountClientService.batchCommand(UUID.randomUUID().toString().replaceAll('-',''),commandLs)
    assertEquals (res.result, 'true')
  }

  void testFreeze() {
    def commandLs = accountClientService.buildFreeze(null, '4560000000000002', 300, 'freeze', '1', '1', 'aaaaaaaaaaaa')
    def res = accountClientService.batchCommand(UUID.randomUUID().toString().replaceAll('-',''),commandLs)
    assertEquals (res.result, 'true')
  }

  void testUnfreeze() {
    def commandLs = accountClientService.buildUnfreeze(null, '4560000000000002', 300, 'unfreeze', '1', '1', 'aaaaaaaaaaaa')
    def res = accountClientService.batchCommand(UUID.randomUUID().toString().replaceAll('-',''),commandLs)
    assertEquals (res.result, 'true')
  }

  void testBatchCommand() {
    def commandLs = accountClientService.buildTransfer(null, '4560000000000004', '4560000000000002', 1000, 'trans', '1', '1', 'aaaaaaaaaaaa')
    commandLs = accountClientService.buildFreeze(commandLs, '4560000000000002', 300, 'freeze', '1', '1', 'aaaaaaaaaaaa')
    commandLs = accountClientService.buildUnfreeze(commandLs, '4560000000000002', 300, 'unfreeze', '1', '1', 'aaaaaaaaaaaa')
    def res = accountClientService.batchCommand('22222221111111',commandLs)
      System.err.println("res = " + res);
    assertEquals (res.result, 'true')

  }
}
