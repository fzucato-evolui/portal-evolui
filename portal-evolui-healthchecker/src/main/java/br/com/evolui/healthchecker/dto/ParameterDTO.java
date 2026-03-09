package br.com.evolui.healthchecker.dto;


import br.com.evolui.healthchecker.enums.ActionType;

public class ParameterDTO {
    private ActionType actionType;


    public static ParameterDTO build(String[] args) throws Exception {
        if(args==null || args.length==0){
            return null;
        }

        ParameterDTO dto = new ParameterDTO();
        String action = args[0].replaceFirst("-", "");
        dto.setActionType(ActionType.valueOf(action));

        return dto;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }
}
