package io.opsit.explang;

public class BankAccount {
  public BankAccount(Integer bank, Integer branch, Integer account) {
    this.bankId = bank;
    this.branchId = branch;
    this.accountNbr = account;
  }
    
  private  Integer  accountNbr;
  private  Integer  branchId;
  private  Integer  bankId;

    
  public Integer getAccountNbr() {
    return accountNbr;
  }
    

  public Integer getBranchId() {
    return branchId;
  }

  public Integer getBankId() {
    return bankId;
  }

  public String toString() {
    return "ACC<bank="+bankId+", branch="+branchId+", accno="+accountNbr+">";
  }

}
