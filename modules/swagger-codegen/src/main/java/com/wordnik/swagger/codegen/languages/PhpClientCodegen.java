package com.wordnik.swagger.codegen.languages;

import com.wordnik.swagger.codegen.*;
import com.wordnik.swagger.models.Swagger;
import com.wordnik.swagger.models.properties.*;

import java.util.*;
import java.io.File;

public class PhpClientCodegen extends DefaultCodegen implements CodegenConfig {
  private String invokerPackage = null;
  protected String groupId = "com.wordnik";
  protected String artifactId = "swagger-client";
  protected String artifactVersion = "1.0.0";

  public CodegenType getTag() {
    return CodegenType.CLIENT;
  }

  public String getName() {
    return "php";
  }

    @Override
  public void preGenerate(Swagger swagger){
    if (swagger.getHost() != null) {
      String namespace = hostToNamespace(swagger.getHost());
      setInvokerPackage(namespace);
    }
    super.preGenerate(swagger);
  }

  public String getInvokerPackage() {
    return invokerPackage;
  }


  class SupportingFileMap extends HashMap<String, SupportingFile> {
      public void add(SupportingFile file) {
        this.put(file.templateFile, file);
      }
  }

  // public for unit testing purposes
  public String hostToNamespace(String hostname) {
    // fix the name space to be reverse, minus io, swap dots for backslash:
    //  db.yoyodyne.com -> yoyodyne/db
    String namespace = "SwaggerPetstore";
    if (hostname != null) {
      if (hostname.indexOf('.') == -1) {
        namespace = hostname;
      } else {
        String[] parts   = hostname.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = parts.length - 2; i >= 0; i--) {
          if (sb.length() > 0) {
            sb.append("\\");
          }
          sb.append(parts[i]);
        }
        namespace = sb.toString();
      }
    }

    return namespace;
  }

  protected String slashReplace(String inVar, String replacement) {
    String outVar = inVar;
    if (inVar != null && inVar.indexOf('\\') != -1) {
      outVar = inVar.replace("\\", replacement);
    }
    return outVar;
  }

  public void setInvokerPackage(String invokerPackage) {

    this.invokerPackage = invokerPackage;

    additionalProperties.put("invokerPackage", invokerPackage);
    additionalProperties.put("escapedInvokerPackage",
            slashReplace(invokerPackage, "\\\\"));
    additionalProperties.put("forwardInvokerPackage",
            slashReplace(invokerPackage, "/"));

    String packagePath = invokerPackage + "-php";

    modelPackage = packagePath + "/lib/models";
    apiPackage = packagePath + "/lib";

    SupportingFileMap sfm = new SupportingFileMap();
    sfm.add(new SupportingFile("composer.mustache", packagePath, "composer.json"));
    sfm.add(new SupportingFile("APIClient.mustache", packagePath + "/lib", "APIClient.php"));
    sfm.add(new SupportingFile("APIClientException.mustache", packagePath + "/lib", "APIClientException.php"));
    sfm.add(new SupportingFile("require.mustache", packagePath, "loader.php"));

    // let's delete anything from supportingFiles which matches.
    // this is in case supportingFiles has been modified outside of
    // this method call
    for (Iterator<SupportingFile> it = supportingFiles.iterator(); it.hasNext() ;) {
        SupportingFile oldFile = it.next();
      if (sfm.containsKey(oldFile.templateFile)) {
          it.remove();
        }
    }
    // now add your new ones back in
    supportingFiles.addAll(sfm.values());
  }


  public String getHelp() {
    return "Generates a PHP client library.";
  }

  public PhpClientCodegen() {
    super();

    setInvokerPackage(camelize("SwaggerClient"));

    outputFolder = "generated-code/php";
    modelTemplateFiles.put("model.mustache", ".php");
    apiTemplateFiles.put("api.mustache", ".php");
    templateDir = "php";

    typeMapping.clear();
    languageSpecificPrimitives.clear();

    reservedWords = new HashSet<String> (
      Arrays.asList(
        "__halt_compiler", "abstract", "and", "array", "as", "break", "callable", "case", "catch", "class", "clone", "const", "continue", "declare", "default", "die", "do", "echo", "else", "elseif", "empty", "enddeclare", "endfor", "endforeach", "endif", "endswitch", "endwhile", "eval", "exit", "extends", "final", "for", "foreach", "function", "global", "goto", "if", "implements", "include", "include_once", "instanceof", "insteadof", "interface", "isset", "list", "namespace", "new", "or", "print", "private", "protected", "public", "require", "require_once", "return", "static", "switch", "throw", "trait", "try", "unset", "use", "var", "while", "xor")
    );

    additionalProperties.put("groupId", groupId);
    additionalProperties.put("artifactId", artifactId);
    additionalProperties.put("artifactVersion", artifactVersion);

    languageSpecificPrimitives.add("int");
    languageSpecificPrimitives.add("array");
    languageSpecificPrimitives.add("map");
    languageSpecificPrimitives.add("string");
    languageSpecificPrimitives.add("DateTime");

    typeMapping.put("long", "int");
    typeMapping.put("integer", "int");
    typeMapping.put("Array", "array");
    typeMapping.put("String", "string");
    typeMapping.put("List", "array");
    typeMapping.put("map", "map");
  }

  @Override
  public String escapeReservedWord(String name) {
    return "_" + name;
  }

  private String dotToSlash(String inVar) {
      return inVar.replace('.', File.separatorChar);
  }

  @Override
  public String apiFileFolder() {
    return outputFolder + "/" + dotToSlash(apiPackage());
  }

  public String modelFileFolder() {
    return outputFolder + "/" + dotToSlash(modelPackage());
  }

  @Override
  public String getTypeDeclaration(Property p) {
    if(p instanceof ArrayProperty) {
      ArrayProperty ap = (ArrayProperty) p;
      Property inner = ap.getItems();
      return getSwaggerType(p) + "[" + getTypeDeclaration(inner) + "]";
    }
    else if (p instanceof MapProperty) {
      MapProperty mp = (MapProperty) p;
      Property inner = mp.getAdditionalProperties();
      return getSwaggerType(p) + "[string," + getTypeDeclaration(inner) + "]";
    }
    return super.getTypeDeclaration(p);
  }

  @Override
  public String getSwaggerType(Property p) {
    String swaggerType = super.getSwaggerType(p);
    String type = null;
    if(typeMapping.containsKey(swaggerType)) {
      type = typeMapping.get(swaggerType);
      if(languageSpecificPrimitives.contains(type)) {
        return type;
      }
    }
    else
      type = swaggerType;
    if(type == null)
      return null;
    return type;
  }

  public String toDefaultValue(Property p) {
    return "null";
  }


  @Override
  public String toVarName(String name) {
    // parameter name starting with number won't compile
    // need to escape it by appending _ at the beginning
    if (name.matches("^[0-9]")) {
      name = "_" + name;
    }
    
    // return the name in underscore style
    // PhoneNumber => phone_number
    return underscore(name);
  }

  @Override
  public String toParamName(String name) {
    // should be the same as variable name
    return toVarName(name);
  }

  @Override
  public String toModelName(String name) {
    // model name cannot use reserved keyword
    if(reservedWords.contains(name))
      escapeReservedWord(name); // e.g. return => _return

    // camelize the model name
    // phone_number => PhoneNumber
    return camelize(name);
  }

  @Override
  public String toModelFilename(String name) {
    // should be the same as the model name
    return toModelName(name);
  }


}
