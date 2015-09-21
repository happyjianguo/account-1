import account.AcSeqAccno
import account.AcSeqTransSeq

class BootStrap {

  def accountService

  def init = { servletContext ->
    //new AcSeqAccno().save()
    //new AcSeqTransSeq().save()
  }

  def destroy = {
  }
}
