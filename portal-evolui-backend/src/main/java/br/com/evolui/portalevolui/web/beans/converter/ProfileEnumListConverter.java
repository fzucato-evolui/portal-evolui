package br.com.evolui.portalevolui.web.beans.converter;

import br.com.evolui.portalevolui.web.beans.enums.ProfileEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.hibernate.internal.util.StringHelper;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Converter
public class ProfileEnumListConverter implements AttributeConverter<List<ProfileEnum>, String> {
    private static final String SPLIT_CHAR = ";";
    Type genericSuperClass;


    @Override
    public String convertToDatabaseColumn(List<ProfileEnum> enumList) {
        return enumList != null ? String.join(SPLIT_CHAR, enumList.stream().map(x -> x.name()).collect(Collectors.toList())) : "";
    }

    @Override
    public List<ProfileEnum> convertToEntityAttribute(String string) {
        if(StringHelper.isEmpty(string)) {
            return emptyList();
        } else {
            List<ProfileEnum> list = new ArrayList<>();

            for (String e : Arrays.asList(string.split(SPLIT_CHAR))) {

                list.add(ProfileEnum.valueOf(e));
            }
            return list;
        }
    }
}
