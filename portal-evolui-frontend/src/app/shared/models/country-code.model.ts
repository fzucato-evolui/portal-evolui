import countries from 'assets/json/country-codes.json';
import {UtilFunctions} from "../util/util-functions";

export class CountryCodeModel {
    private static countryCodes: CountryCodeModel[] = countries;
    public name: string;
    public dial_code: string;
    public code: string;

    public static getFromDialCode(dial_code: string): CountryCodeModel {
        dial_code = UtilFunctions.removeNonNumbers(dial_code);
        if (UtilFunctions.isValidStringOrArray(dial_code) === true) {
            if (dial_code === "1") {
                return CountryCodeModel.countryCodes.find(x => x.code === 'US');
            }
            return CountryCodeModel.countryCodes.find(x => x.dial_code === '+' + dial_code);
        }
        return null;
    }
}
