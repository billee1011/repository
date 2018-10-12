package com.cai.game.mj.state;

public class StateMachine<T> {
	private IState<T> _prevState;
	private IState<T> _crntState;
	private T _owner;
	
	public StateMachine(T owner){
		_prevState=null;
		_crntState=null;
		_owner=owner;
	}
	public void set_current_state(IState<T> state){
		this._crntState = state;

		if (this._crntState!=null) {
			this._crntState.on_enter(_owner);
		}
	}
	
	public void change_state(IState<T> state){
		_crntState.on_exit(_owner);
		_prevState = _crntState;
		_crntState = state;
		if(_crntState!=null)_crntState.on_enter(_owner);
	}
	
	public void on_message(long account_id,com.google.protobuf.GeneratedMessage msg){
		if (this._crntState!=null) {
			_crntState.on_message(_owner,account_id,msg);
		}
	}
	
	public void on_global(long account_id,com.google.protobuf.GeneratedMessage msg){
		if (this._crntState!=null) {
			_crntState.on_global(_owner,account_id,msg);
		}
	}
	
}
