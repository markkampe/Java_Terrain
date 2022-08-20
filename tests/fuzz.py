"""
Read JSON world description, round specified attribute to a specified precision
"""
from optparse import OptionParser
from sys import stdin, stdout, stderr, exit


def process(infile, attribute, places):
    """
    process the input file, looking for the specified attribute, and
    wherever it is found round it to the specified precision
    """
    # figure out the rounding sum and printing format
    fmt = "{:." + str(places) + "f}"

    # process the input file
    in_points = False
    key = '"' + attribute + '"' + ": "
    for line in infile:
        text = line.strip()

        # wait until we get into the point descriptions
        if text.startswith("\"points\": ["):
            stdout.write(line)
            in_points = True
            continue

        # if not in the point descriptions, just print the line
        if not in_points:
            stdout.write(line)
            continue

        # look for lines that contain the specified attribute
        key_start = line.find(key)
        if key_start >= 0:
            # pull off the various parts of the line
            value_start = key_start + len(key)
            value_end = line.find(',', value_start)
            before = line[0:value_start]
            value = float(line[value_start:value_end])
            after = line[value_end:]

            # see if we have rounded it to zero
            new_value = fmt.format(value)
            if float(new_value) == 0.0:
                # leave out the zeroed attribute (and its comma & space)
                stdout.write(line[0:key_start] + line[value_end+2:])
            else:
                stdout.write(before + new_value + after)
        else:   # if not, just print the line as is
            stdout.write(line)

        # note when we exit the point descriptions
        if text.endswith("],"):
            in_points = False
            continue


if __name__ == "__main__":
    # process the comand line arguments
    msg = "usage: %prog -a attribute [-p #places] [input_file]"
    parser = OptionParser(usage=msg)
    parser.add_option("-a", "--attribute", action="store",
                      type="string", dest="attribute")
    parser.add_option("-p", "--places", action="store",
                      type="int", dest="places", default=6)
    (opts, files) = parser.parse_args()

    if opts.attribute is None:
        stderr.write("ERROR no attribute name specified\n")
        stderr.write(msg + "\n")
        exit(-1)

    if len(files) > 0:  # process the named input file
        try:
            with open(files[0], 'r') as infile:
                process(infile, opts.attribute, opts.places)
            infile.close()
            exit(0)
        except Exception as e:
            stderr.write("Unable to open world description: " + files[0] +
                         "\n")
            stderr.write(e.message, "\n")
            exit(-1)
    else:               # process stdin
        process(stdin, opts.attribute, opts.places)
        exit(0)
