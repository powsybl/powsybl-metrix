import os, sys
import numpy as np

key_words = {"ROWS", "COLUMNS", "RHS", "BOUNDS", "ENDATA"}
mps_suffix = ".mps"

# {Name : [sense, {Var: coeff}}
names = {}
matrix = {}
bounds = {}
ctr_count = 0
var_count = 0

def round_coeff(coeff):
    return coeff
    if round(coeff,2) != 0.:
        return round(coeff,2)
    else:
        print("coeff = "+str(coeff))
        coeff_str = str(coeff).split('.')[1]        
        non_null = next(x for x in coeff_str if x != '0')
        index = coeff_str.index(non_null)
        res_str = non_null+"e-"+str(index+1)
        res = np.format_float_scientific(float(res))
        print("res = "+str(res))
        return res

def convert_sense(sense_str):
    if sense_str == "N":
        return "min"
    elif sense_str == "G":
        return ">="
    elif sense_str == "L":
        return "<="
    elif sense_str == "E":
        return "="

def read_line(key_count, line):
    global matrix, bounds, names, ctr_count, var_count
    space_content = line.split(" ")
    content  = [x for x in space_content if x != ""]
    if key_count == 1:
        ctr_or_obj_name = "OBJECTIF" 
        if content[1] != "OBJECTIF":
            ctr_or_obj_name = "ctr_"+str(ctr_count)
            ctr_count += 1
        names[content[1]] = ctr_or_obj_name
        matrix[ctr_or_obj_name] = [convert_sense(content[0])]
    elif key_count == 2:
        ctr_or_obj_name = names[content[1]]
        if len(matrix[ctr_or_obj_name]) <= 1:
            matrix[ctr_or_obj_name].append(dict())
        if content[0] in names:
            var_name = names[content[0]]
        else :
            var_name = "x_"+str(var_count)
            var_count += 1
            names[content[0]] = var_name
        matrix[ctr_or_obj_name][1][var_name] = round_coeff(float(content[2]))
    elif key_count == 3:
        ctr_or_obj_name = names[content[1]]
        matrix[ctr_or_obj_name][1][content[0]] = content[2]
    elif key_count == 4:
        if content[2] in names:
            var_name = names[content[2]]
        else :
            var_name = "x_"+str(var_count)
            var_count += 1
            names[content[2]] = var_name
        bounds[var_name] = [content[0], content[3] if len(content) >= 4 else 1]

def write_lp_file(dirname, mps_filename):
    new_filename = "lp_"+mps_filename.split(".mps")[0]+".txt"
    new_file = open(os.path.join(dirname, new_filename), "w")
    new_file.write("Variables:\n")
    for var in bounds :
        new_file.write("\t"+str(var)+" "+str(bounds[var][0])+" <= "+str(bounds[var][1])+"\n")
    new_file.write("Objective:\n")
    obj_str = "\t"+str(matrix["OBJECTIF"][0])
    add_str = " "
    for var in matrix["OBJECTIF"][1]:
        if var != "RHSVAL":
            obj_str += add_str + str(matrix["OBJECTIF"][1][var])+"*"+str(var)
            add_str = " + "
    new_file.write(obj_str+"\n")
    new_file.write("Constraints:\n")
    for ctr in matrix:
        if ctr != "OBJECTIF":
            ctr_str = "\t"+str(ctr)+":"
            add_str = " "
            end_str = ""
            for var in matrix[ctr][1]:
                if var != "RHSVAL" :
                    ctr_str += add_str + str(matrix[ctr][1][var])+"*"+str(var)
                    add_str = " + "
                else:
                    end_str = " " + str(matrix[ctr][0]) + " " + str(matrix[ctr][1][var])
            new_file.write(ctr_str+end_str+"\n")
    new_file.close() 

if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1].endswith(mps_suffix):
        full_mps_filename = str(sys.argv[1])
        mps_filename = os.path.basename(full_mps_filename)
        dirname = os.path.dirname(full_mps_filename)
        mps_file = open(full_mps_filename, "r")
        key_count = 0
        for line in mps_file:
            line = line.split("\n")[0]
            if line in key_words:
                key_count += 1
            elif key_count > 0:
                read_line(key_count, line)
        mps_file.close()
        write_lp_file(dirname, mps_filename)       
    else:
        print("You must specify a MPS file!")
