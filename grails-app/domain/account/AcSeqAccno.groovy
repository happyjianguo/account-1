package account

class AcSeqAccno {

  static mapping = {
    id generator: 'sequence', params: [sequence: 'seq_ac_accno']
  }
  static constraints = {
  }
}
