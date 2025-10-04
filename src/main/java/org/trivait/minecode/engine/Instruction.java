package org.trivait.minecode.engine;

public class Instruction {
    public enum Type {
        SAY, WAIT, LOOK, WALK, STOP,
        VAR_DECL, VAR_SET,
        IF, ELSE, ENDIF,
        WHILE, ENDWHILE,
        DO, WHILE_AFTER_DO,
        FOR, ENDFOR,
        SWITCH, CASE, DEFAULT, ENDSWITCH
    }

    public final Type type;
    public final String text;
    public final String text2;
    public final String text3;
    public final int ticks;
    public final float yaw, pitch;
    public final int walkDirection;

    private Instruction(Type type, String text, String text2, String text3,
                        int ticks, float yaw, float pitch, int walkDirection) {
        this.type = type;
        this.text = text;
        this.text2 = text2;
        this.text3 = text3;
        this.ticks = ticks;
        this.yaw = yaw;
        this.pitch = pitch;
        this.walkDirection = walkDirection;
    }

    // Игровые
    public static Instruction say(String t) { return new Instruction(Type.SAY, t,null,null,0,0,0,0); }
    public static Instruction waitTicks(int t) { return new Instruction(Type.WAIT,null,null,null,t,0,0,0); }
    public static Instruction look(float yaw,float pitch){return new Instruction(Type.LOOK,null,null,null,0,yaw,pitch,0);}
    public static Instruction walkForward(int ticks){int dir=ticks>=0?1:-1;return new Instruction(Type.WALK,null,null,null,Math.abs(ticks),0,0,dir);}
    public static Instruction stop(){return new Instruction(Type.STOP,null,null,null,0,0,0,0);}

    // Переменные
    public static Instruction declareVar(String type,String name,String init){return new Instruction(Type.VAR_DECL,name,type,init,0,0,0,0);}
    public static Instruction setVar(String name,String expr){return new Instruction(Type.VAR_SET,name,expr,null,0,0,0,0);}

    // Управляющие
    public static Instruction ifCond(String cond){return new Instruction(Type.IF,cond,null,null,0,0,0,0);}
    public static Instruction elseBlock(){return new Instruction(Type.ELSE,null,null,null,0,0,0,0);}
    public static Instruction endIf(){return new Instruction(Type.ENDIF,null,null,null,0,0,0,0);}
    public static Instruction whileLoop(String cond){return new Instruction(Type.WHILE,cond,null,null,0,0,0,0);}
    public static Instruction endWhile(){return new Instruction(Type.ENDWHILE,null,null,null,0,0,0,0);}
    public static Instruction doBlock(){return new Instruction(Type.DO,null,null,null,0,0,0,0);}
    public static Instruction whileAfterDo(String cond){return new Instruction(Type.WHILE_AFTER_DO,cond,null,null,0,0,0,0);}
    public static Instruction forLoop(String init,String cond,String update){return new Instruction(Type.FOR,init,cond,update,0,0,0,0);}
    public static Instruction endFor(){return new Instruction(Type.ENDFOR,null,null,null,0,0,0,0);}
    public static Instruction switchExpr(String expr){return new Instruction(Type.SWITCH,expr,null,null,0,0,0,0);}
    public static Instruction caseExpr(String expr){return new Instruction(Type.CASE,expr,null,null,0,0,0,0);}
    public static Instruction defaultCase(){return new Instruction(Type.DEFAULT,null,null,null,0,0,0,0);}
    public static Instruction endSwitch(){return new Instruction(Type.ENDSWITCH,null,null,null,0,0,0,0);}
}
