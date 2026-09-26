import os
import zipfile
root = os.path.expanduser(r'~/.m2/repository/org/springframework/batch')
search = ['JobBuilderFactory.class', 'StepBuilderFactory.class', 'JobExecutionListenerSupport.class']
found = False
for dirpath, _, filenames in os.walk(root):
    for fn in filenames:
        if fn.endswith('.jar'):
            path = os.path.join(dirpath, fn)
            try:
                with zipfile.ZipFile(path, 'r') as z:
                    names = set(z.namelist())
                    for s in search:
                        if s in names:
                            print(path, s)
                            found = True
            except Exception:
                pass
if not found:
    print('None found')
